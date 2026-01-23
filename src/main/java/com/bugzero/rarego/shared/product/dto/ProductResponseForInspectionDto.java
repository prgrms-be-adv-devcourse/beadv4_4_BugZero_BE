package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;

import lombok.Builder;

@Builder
public record ProductResponseForInspectionDto(
	Long ProductId,
	String name,
	String sellerEmail,
	Category category,
	InspectionStatus inspectionStatus,
	String thumbnail
) {
}
