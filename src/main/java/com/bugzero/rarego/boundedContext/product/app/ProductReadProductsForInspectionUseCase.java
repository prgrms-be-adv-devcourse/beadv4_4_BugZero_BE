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

	@Transactional(readOnly = true)
	public PagedResponseDto<ProductResponseForInspectionDto> readProducts(
		ProductSearchForInspectionCondition condition, Pageable pageable
	) {
		Page<ProductResponseForInspectionDto> productDtos = productRepository.
			readProductsForAdmin(condition.name(), condition.category(), condition.status(), pageable);

		return PagedResponseDto.from(productDtos);
	}
}
