package com.bugzero.rarego.boundedContext.product.domain;

import java.util.List;

public record ProductRequestDto(
	String name,
	Category category,
	String description,
	ProductAuctionRequestDto productAuctionRequestDto,
	List<ProductImageRequestDto> productImageRequestDto
) {
}
