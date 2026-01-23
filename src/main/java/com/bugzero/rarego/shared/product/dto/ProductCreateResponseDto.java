package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;

import lombok.Builder;

@Builder
public record ProductCreateResponseDto(
	long productId,
	long auctionId,
	InspectionStatus inspectionStatus
) {
}
