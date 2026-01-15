package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductImageRequestDto(
	@NotBlank(message = "이미지 url 정보가 필요합니다.")
	String imgUrl,
	@PositiveOrZero(message = "사진 순서는 음수값을 사용하지 못합니다.")
	int sortOrder
) {
	public ProductImage toEntity(Product product) {
		return ProductImage.builder()
			.product(product)
			.imageUrl(imgUrl)
			.sortOrder(sortOrder)
			.build();
	}
}
