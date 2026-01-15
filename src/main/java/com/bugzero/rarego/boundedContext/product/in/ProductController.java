package com.bugzero.rarego.boundedContext.product.in;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.product.app.ProductFacade;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductFacade  productFacade;

	@PostMapping
	public SuccessResponseDto<ProductResponseDto> createProduct(
		@RequestParam long memberId,
		@Valid @RequestBody ProductRequestDto productRequestDto) {
		ProductResponseDto responseDto = productFacade.createProduct(memberId, productRequestDto);
		return SuccessResponseDto.from(SuccessType.CREATED, responseDto);
	}
}
