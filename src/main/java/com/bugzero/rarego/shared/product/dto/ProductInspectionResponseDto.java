package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.bounded_context.product.domain.InspectionStatus;
import com.bugzero.rarego.bounded_context.product.domain.ProductCondition;

import lombok.Builder;

@Builder
public record ProductInspectionResponseDto(
	Long inspectionId,
	Long productId,
	InspectionStatus newStatus,
	ProductCondition productCondition,
	String reason
) {
}
