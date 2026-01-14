package com.bugzero.rarego.boundedContext.product.domain;

public record ProductAuctionRequestDto(
	int startPrice,
	int durationDays
) {
}
