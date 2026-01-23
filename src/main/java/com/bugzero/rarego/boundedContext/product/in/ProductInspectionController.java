package com.bugzero.rarego.boundedContext.product.in;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.product.app.ProductFacade;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.product.dto.ProductInspectionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductInspectionResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseForInspectionDto;
import com.bugzero.rarego.shared.product.dto.ProductSearchForInspectionCondition;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/inspections")
@Tag(name = "Product_Inspection", description = "상품 검수 관련 API")
public class ProductInspectionController {
	private final ProductFacade  productFacade;

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "상품 검수", description = "관리자가 상품 검수에 대해 승인/반려 처리를 합니다.")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public SuccessResponseDto<ProductInspectionResponseDto> createProductInspection(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Valid @RequestBody ProductInspectionRequestDto productInspectionRequestDto) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			productFacade.createInspection(principal.publicId(), productInspectionRequestDto));
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "상품 검수정보 상세조회", description = "관리자가 특정 상품의 검수 상세정보를 조회합니다.")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/{productId}")
	public SuccessResponseDto<ProductInspectionResponseDto> readInspection(
		@PathVariable Long productId
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			productFacade.readInspection(productId));
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "검수를 위한 전체 상품목록 조회", description = "관리자가 상품 검수 처리를 위해 모든 상품정보를 조회합니다.")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public SuccessResponseDto<PagedResponseDto<ProductResponseForInspectionDto>> getAdminProducts(
		ProductSearchForInspectionCondition condition,
		@PageableDefault(
			size = 10,
			sort = "createdAt",
			direction = Sort.Direction.DESC
		) Pageable pageable
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			productFacade.readProductsForInspection(condition, pageable));
	}
}
