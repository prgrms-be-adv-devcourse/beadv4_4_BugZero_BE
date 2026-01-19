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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "상품 관련 API")
public class ProductController {

	private final ProductFacade  productFacade;

	@Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다")
	@PostMapping
	public SuccessResponseDto<ProductResponseDto> createProduct(
		@RequestParam String memberUUID,
		@Valid @RequestBody ProductRequestDto productRequestDto) {
		ProductResponseDto responseDto = productFacade.createProduct(memberUUID, productRequestDto);
		return SuccessResponseDto.from(SuccessType.CREATED, responseDto);
	}
}
