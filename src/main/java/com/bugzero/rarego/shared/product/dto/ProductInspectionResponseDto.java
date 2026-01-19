package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;

import lombok.Builder;

@Builder
public record ProductInspectionResponseDto(
	Long inspectionId,
	Long productId,
	InspectionStatus newStatus,
	String reason
) {
}
