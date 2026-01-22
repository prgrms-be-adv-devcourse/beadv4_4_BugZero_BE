package com.bugzero.rarego.boundedContext.product.in;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.product.app.ProductFacade;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.product.dto.ProductInspectionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductInspectionResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseForInspectionDto;
import com.bugzero.rarego.shared.product.dto.ProductSearchForInspectionCondition;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/inspections")
@Tag(name = "Product_Inspection", description = "상품 검수 관련 API")
public class ProductInspectionController {
	private final ProductFacade  productFacade;

	//TODO 추후 리팩토리 시 auth적용
	@PostMapping
	public SuccessResponseDto<ProductInspectionResponseDto> createProductInspection(
		@RequestParam String inspectorId,
		@Valid @RequestBody ProductInspectionRequestDto productInspectionRequestDto) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			productFacade.createInspection(inspectorId, productInspectionRequestDto));
	}


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

	//상품 별 검수 정보(검수 반려 이유등)를 알 수 있는 api

}
