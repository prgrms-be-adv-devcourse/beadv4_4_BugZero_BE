package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;

public record ProductResponseDto (
	long productId,
	long auctionId,
	InspectionStatus inspectionStatus
) {
}
