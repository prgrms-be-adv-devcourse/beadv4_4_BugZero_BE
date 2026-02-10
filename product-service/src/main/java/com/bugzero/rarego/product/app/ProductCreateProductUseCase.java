package com.bugzero.rarego.product.app;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductImage;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.domain.dto.ProductCreateResponseDto;
import com.bugzero.rarego.product.out.ProductRepository;
import com.bugzero.rarego.shared.auction.event.AuctionManagementEvent;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCreateProductUseCase {
	private final ProductRepository productRepository;
	private final AuctionApiClient auctionApiClient;
	private final ProductSupport productSupport;
	private final EventPublisher eventPublisher;
	private final ObjectMapper objectMapper;

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Transactional
    public ProductCreateResponseDto createProduct(String publicId, ProductCreateRequestDto dto) {

		ProductMember seller = productSupport.verifyValidateMember(publicId);

		Product product = confirmImages(Product.createProduct(seller, dto.name(), dto.category(), dto.description()),
			dto.productImageRequestDto());

        // 부모만 저장 (CascadeType.PERSIST에 의해 자식인 ProductImage들도 자동으로 INSERT됨)
        Product savedProduct = productRepository.save(product);


		//TODO 아웃박스 패턴 도입 시 아웃박스 테이블에 발행할 이벤트를 저장하는 로직으로 변경
		try {
			String payload = objectMapper.writeValueAsString(dto.productAuctionRequestDto());

			AuctionManagementEvent event = new AuctionManagementEvent(
				AuctionProductEventType.CREATE, // eventType
				"REQ-" + UUID.randomUUID(),    // requestId
				savedProduct.getId(),                           // productId
				publicId,               // publicId
				payload                     // payload
			);

			kafkaTemplate.send("auction-product-events", event.productId().toString(), event);
		} catch (JsonProcessingException e) {
			log.error("errorCode: {}, message: {}",
				ErrorType.JSON_PARSING_FAILED.getCode(),
				ErrorType.JSON_PARSING_FAILED.getMessage());
			throw new CustomException(ErrorType.JSON_PARSING_FAILED);
		}

		return ProductCreateResponseDto.builder()
			.productId(savedProduct.getId())
			.inspectionStatus(savedProduct.getInspectionStatus())
			.build();
	}

	//상품 이미지 url 저장
	private Product confirmImages(Product product, List<ProductImageRequestDto> dtos) {
		//비동기 처리를 위해 원본 temp 경로들을 저장할 리스트
		List<String> tempPaths = new ArrayList<>();

		//상품 이미지 순서 보장 정렬 후 저장
		List<ProductImageRequestDto> images = productSupport.normalizeCreateImageOrder(dtos);

		images.forEach(imageRequestDto -> {
			String originalTempPath = imageRequestDto.imgUrl(); // "temp/uuid_lego.jpg"
			tempPaths.add(originalTempPath);

			product.addImage(ProductImage.createConfirmedImage(product, imageRequestDto.imgUrl(), imageRequestDto.sortOrder()));
		});

		// S3 파일 이동 비동기 호출 (원본 temp 경로 리스트 전달 -> 확정이미지만 S3 product 경로로 옮김)
		eventPublisher.publish(new S3ImageConfirmEvent(tempPaths));

		return product;
	}
}
