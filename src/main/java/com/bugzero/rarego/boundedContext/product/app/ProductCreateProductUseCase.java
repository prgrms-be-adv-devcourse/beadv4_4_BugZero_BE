package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCreateProductUseCase {
	private final ProductRepository productRepository;

	@Transactional
	public ProductResponseDto createProduct(long memberId, ProductRequestDto productRequestDto) {

		//TODO 멤버id가 아닌 멤버를 직접받을 수 있도록 변경
		Product product = productRequestDto.toEntity(memberId);

		//상품 이미지 정보 저장
		productRequestDto.productImageRequestDto().forEach(imageRequestDto -> {
			product.addImage(imageRequestDto.toEntity(product));
		});

		// 부모만 저장 (CascadeType.PERSIST에 의해 자식인 ProductImage들도 자동으로 INSERT됨)
		Product savedProduct = productRepository.save(product);

		//TODO 경매정보 생성 요청하는 api

		//TODO 검수 요청하는 api or event

		return ProductResponseDto.builder()
			.productId(savedProduct.getId())
			.auctionId(1)
			.inspectionStatus(savedProduct.getInspectionStatus())
			.build();
	}
}
