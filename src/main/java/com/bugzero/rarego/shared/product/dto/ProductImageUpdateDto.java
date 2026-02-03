package com.bugzero.rarego.shared.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductImageUpdateDto(
	Long id, // 기존 이미지라면 id값이 있고 없다면 null
	@NotBlank
	String imgUrl,
	@PositiveOrZero(message = "사진 순서는 음수값을 사용하지 못합니다.")
	int sortOrder
) {
	public ProductImageUpdateDto withOrder(int newOrder) {
		return new ProductImageUpdateDto(this.id, this.imgUrl, newOrder);
	}
}
