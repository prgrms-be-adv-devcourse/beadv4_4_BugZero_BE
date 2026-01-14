package com.bugzero.rarego.boundedContext.product.domain;

public record ProductImageRequestDto(
	boolean thumbnail,
	int sortOrder
) {
}
