package com.bugzero.rarego.boundedContext.product.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.shared.product.dto.ProductInspectionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductInspectionResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductFacade {

	private final ProductCreateProductUseCase productCreateProductUseCase;
	private final ProductCreateInspectionUseCase productCreateInspectionUseCase;

	public ProductResponseDto createProduct(String memberUUID, ProductRequestDto dto) {
		return productCreateProductUseCase.createProduct(memberUUID, dto);
	}

	public ProductInspectionResponseDto createInspection(String memberUUID, ProductInspectionRequestDto dto) {
		return productCreateInspectionUseCase.createInspection(memberUUID, dto);
	}

}
