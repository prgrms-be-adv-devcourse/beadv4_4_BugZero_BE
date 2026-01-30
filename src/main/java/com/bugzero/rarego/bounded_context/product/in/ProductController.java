package com.bugzero.rarego.bounded_context.product.in;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.bounded_context.product.app.ProductFacade;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateResponseDto;

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
		@RequestParam String publicId,
		@Valid @RequestBody ProductRequestDto productRequestDto) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			productFacade.createProduct(publicId, productRequestDto));
	}

	@Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
	@PatchMapping("/{productId}")
	public SuccessResponseDto<ProductUpdateResponseDto> updateProduct(
		@RequestParam String publicId,
		@PathVariable Long productId,
		@Valid @RequestBody ProductUpdateDto productUpdateDto
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			productFacade.updateProduct(publicId, productId, productUpdateDto));
	}

}
