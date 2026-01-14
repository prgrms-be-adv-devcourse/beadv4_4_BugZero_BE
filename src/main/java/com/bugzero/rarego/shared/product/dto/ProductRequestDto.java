package com.bugzero.rarego.shared.product.dto;

import java.util.List;

import com.bugzero.rarego.boundedContext.product.domain.Category;

public record ProductRequestDto(
	String name,
	Category category,
	String description,
	ProductAuctionRequestDto productAuctionRequestDto,
	List<ProductImageRequestDto> productImageRequestDto
) {
}
