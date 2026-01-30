package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseForInspectionDto;
import com.bugzero.rarego.shared.product.dto.ProductSearchForInspectionCondition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductReadProductsForInspectionUseCase {
	private final ProductRepository productRepository;
	private final ProductImageS3UseCase s3PresignerUrlUseCase;

	@Transactional(readOnly = true)
	public PagedResponseDto<ProductResponseForInspectionDto> readProducts(
		ProductSearchForInspectionCondition condition, Pageable pageable
	) {
		Page<ProductResponseForInspectionDto> productDtos = productRepository.
			readProductsForAdmin(condition.name(), condition.category(), condition.status(), pageable);

		return PagedResponseDto.from(productDtos, this::toPresignedDto);
	}

	private ProductResponseForInspectionDto toPresignedDto(ProductResponseForInspectionDto dto) {
		return new ProductResponseForInspectionDto(
			dto.ProductId(),
			dto.name(),
			dto.sellerEmail(),
			dto.category(),
			dto.inspectionStatus(),
			s3PresignerUrlUseCase.getPresignedGetUrl(dto.thumbnail())
		);
	}
}
