package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductFacade {

	private final ProductCreateProductUseCase productCreateProductUseCase;

	public ProductResponseDto createProduct(String memberUUID, ProductRequestDto productRequestDto) {
		return productCreateProductUseCase.createProduct(memberUUID, productRequestDto);
	}
}
