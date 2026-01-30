package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.bounded_context.product.domain.Inspection;
import com.bugzero.rarego.bounded_context.product.domain.InspectionStatus;
import com.bugzero.rarego.bounded_context.product.domain.Product;
import com.bugzero.rarego.bounded_context.product.domain.ProductCondition;
import com.bugzero.rarego.bounded_context.product.domain.ProductMember;

import jakarta.validation.constraints.NotNull;

public record ProductInspectionRequestDto(
	@NotNull(message = "상품 id는 필수입니다.")
	Long productId,
	@NotNull(message = "검수 결과는 필수입니다.")
	InspectionStatus status,
	@NotNull(message = "상품 상태값은 필수입니다.")
	ProductCondition productCondition,
	String reason
) {
	public Inspection toEntity(Product product, ProductMember seller, Long inspectorId) {
		return Inspection.builder()
			.product(product)
			.seller(seller)
			.inspectorId(inspectorId)
			.inspectionStatus(status)
			.productCondition(productCondition)
			.reason(reason)
			.build();
	}

}
