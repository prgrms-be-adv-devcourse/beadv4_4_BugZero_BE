package com.bugzero.rarego.shared.product.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ProductAuctionResponseDto(
	Long id,
	Long sellerId,
	String name,
	String description,
	String category,
	String thumbnailUrl,
	List<String> imageUrls
) {
}
