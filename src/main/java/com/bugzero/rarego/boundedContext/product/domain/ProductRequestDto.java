package com.bugzero.rarego.boundedContext.product.domain;

public record ProductRequestDto(
	String name,
	Category category,
	String description,
	ProductAuctionRequestDto productAuctionRequestDto,
	ProductImageRequestDto productImageRequestDto
) {
}
