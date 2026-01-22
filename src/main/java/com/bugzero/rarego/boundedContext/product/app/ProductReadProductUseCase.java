package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseForInspectionDto;
import com.bugzero.rarego.shared.product.dto.ProductSearchForInspectionCondition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductReadProductUseCase {
	private final ProductRepository productRepository;

	public PagedResponseDto<ProductResponseForInspectionDto> readProducts(
		ProductSearchForInspectionCondition condition, Pageable pageable
	) {
		Page<ProductResponseForInspectionDto> productDtos = productRepository.
			readProductsForAdmin(condition.name(), condition.category(), condition.status(), pageable);

		return PagedResponseDto.from(productDtos);
	}
}
