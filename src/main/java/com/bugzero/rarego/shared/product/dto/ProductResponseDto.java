package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.bounded_context.product.domain.InspectionStatus;

import lombok.Builder;

@Builder
public record ProductResponseDto (
	long productId,
	long auctionId,
	InspectionStatus inspectionStatus
) {
}
