package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.boundedContext.product.domain.Inspection;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;

import jakarta.validation.constraints.NotNull;

public record ProductInspectionRequestDto(
	@NotNull(message = "상품 id는 필수입니다.")
	Long productId,
	@NotNull(message = "검수 결과는 필수입니다.")
	InspectionStatus status,
	String reason
) {
	public Inspection toEntity(Product product, ProductMember seller, Long inspectorId) {
		return Inspection.builder()
			.product(product)
			.seller(seller)
			.inspectorId(inspectorId)
			.status(status)
			.reason(reason)
			.build();
	}

}
