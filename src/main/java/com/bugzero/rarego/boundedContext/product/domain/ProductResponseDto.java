package com.bugzero.rarego.boundedContext.product.domain;

public record ProductResponseDto (
	long productId,
	long auctionId,
	InspectionStatus inspectionStatus
) {
}
