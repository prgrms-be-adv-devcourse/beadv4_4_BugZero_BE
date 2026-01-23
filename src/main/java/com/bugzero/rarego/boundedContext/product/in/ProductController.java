package com.bugzero.rarego.boundedContext.product.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.product.app.ProductFacade;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductRequestResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "상품 관련 API")
public class ProductController {

	private final ProductFacade productFacade;

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다")
	@PreAuthorize("hasRole('SELLER')")
	@PostMapping
	public SuccessResponseDto<ProductRequestResponseDto> createProduct(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Valid @RequestBody ProductRequestDto productRequestDto) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			productFacade.createProduct(principal.publicId(), productRequestDto));
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
	@PreAuthorize("hasRole('SELLER')")
	@PatchMapping("/{productId}")
	public SuccessResponseDto<ProductUpdateResponseDto> updateProduct(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable Long productId,
		@Valid @RequestBody ProductUpdateDto productUpdateDto
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			productFacade.updateProduct(principal.publicId(), productId, productUpdateDto));
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "상품 삭제", description = "상품 정보를 삭제합니다.")
	@PreAuthorize("hasRole('SELLER')")
	@DeleteMapping("/{productId}")
	public SuccessResponseDto<Void> deleteProduct(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable Long productId
	) {
		productFacade.deleteProduct(principal.publicId(), productId);
		return SuccessResponseDto.from(SuccessType.OK);
	}

}
