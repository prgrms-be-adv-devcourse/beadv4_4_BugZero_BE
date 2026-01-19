package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.product.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCreateProductUseCase {
	private final ProductRepository productRepository;
	private final AuctionApiClient auctionApiClient;

	@Transactional
	public ProductResponseDto createProduct(String memberUUID, ProductRequestDto productRequestDto) {

		//TODO UUID를 통해 멤버 pk ID 값을 구하여 전달
		Long memberId = 1L;
		Product product = productRequestDto.toEntity(memberId);

		//상품 이미지 정보 저장
		productRequestDto.productImageRequestDto().forEach(imageRequestDto -> {
			product.addImage(imageRequestDto.toEntity(product));
		});

		// 부모만 저장 (CascadeType.PERSIST에 의해 자식인 ProductImage들도 자동으로 INSERT됨)
		Product savedProduct = productRepository.save(product);

		//경매정보 생성 요청하는 api
		Long auctionId = auctionApiClient.createAuction(savedProduct.getId(), memberUUID,
			productRequestDto.productAuctionRequestDto());

		//TODO 검수 요청하는 api or event -> 검수 요청 이벤트는 필요없을 거 같음. 단순히 검수 대기 중인 목록을 조회하면 어떤 상품을 검수해야 하는지 알 수 있기 때문에.
		//

		return ProductResponseDto.builder()
			.productId(savedProduct.getId())
			.auctionId(auctionId)
			.inspectionStatus(savedProduct.getInspectionStatus())
			.build();
	}
}
