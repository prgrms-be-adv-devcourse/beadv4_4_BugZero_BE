package com.bugzero.rarego.product.app;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductImage;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.domain.dto.ProductCreateResponseDto;
import com.bugzero.rarego.product.out.ProductRepository;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCreateProductUseCase {
	private final ProductRepository productRepository;
	private final ProductSupport productSupport;
	private final EventPublisher eventPublisher;
	private final ProductOutboxSupport productOutboxSupport;

	@Transactional
	public ProductCreateResponseDto createProduct(String publicId, ProductCreateRequestDto dto) {

		// 1. 데이터 검증 및 가공 (엔티티 생성)
		ProductMember seller = productSupport.verifyValidateMember(publicId);
		Product product = Product.createProduct(seller, dto.name(), dto.category(), dto.description());

		// 이미지 처리 로직 (여기서 이벤트를 바로 발행하지 않고 정보만 취합)
		List<String> tempPaths = collectImageTempPaths(product, dto.productImageRequestDto());

		// 2. 핵심 비즈니스 데이터 저장 (상품 정보)
		// Cascade에 의해 이미지 엔티티들도 같이 저장됨
		Product savedProduct = productRepository.save(product);

		// 3. 아웃박스 이벤트 저장 (DB 트랜잭션의 일부)
		productOutboxSupport.saveOutbox(savedProduct.getId(), publicId,
			AuctionProductEventType.CREATE, dto.productAuctionRequestDto());

		// 4. 외부 시스템 연동 이벤트 발행 (커밋 후 실행될 녀석들)
		// 모든 DB 저장이 완벽하게 호출된 후, 마지막에 이벤트를 발행하는 것이 흐름상 명확함
		eventPublisher.publish(new S3ImageConfirmEvent(tempPaths));

		return ProductCreateResponseDto.builder()
			.productId(savedProduct.getId())
			.inspectionStatus(savedProduct.getInspectionStatus())
			.build();
	}

	private List<String> collectImageTempPaths(Product product, List<ProductImageRequestDto> dtos) {
		//상품 이미지 순서 보장 정렬 후 저장
		List<ProductImageRequestDto> images = productSupport.normalizeCreateImageOrder(dtos);
		List<String> tempPaths = new ArrayList<>();

		images.forEach(imageRequestDto -> {
			tempPaths.add(imageRequestDto.imgUrl());
			product.addImage(ProductImage.createConfirmedImage(product, imageRequestDto.imgUrl(), imageRequestDto.sortOrder()));
		});
		return tempPaths;
	}
}
