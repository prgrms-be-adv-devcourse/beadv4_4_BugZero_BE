package com.bugzero.rarego.shared.product.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;

import lombok.Builder;

@Builder
public record ProductInspectionResponseDto(
	Long inspectionId,
	Long productId,
	InspectionStatus newStatus,
	ProductCondition productCondition,
	String reason,
	LocalDateTime updatedAt,
	LocalDateTime createdAt
) {
}
