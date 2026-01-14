package com.bugzero.rarego.shared.product.dto;

public record ProductAuctionRequestDto(
	int startPrice,
	int durationDays
) {
}
