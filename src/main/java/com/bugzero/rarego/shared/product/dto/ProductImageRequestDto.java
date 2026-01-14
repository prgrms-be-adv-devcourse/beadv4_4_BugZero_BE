package com.bugzero.rarego.shared.product.dto;

public record ProductImageRequestDto(
	boolean thumbnail,
	int sortOrder
) {
}
