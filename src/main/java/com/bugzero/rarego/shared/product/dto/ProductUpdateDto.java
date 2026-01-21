package com.bugzero.rarego.shared.product.dto;

import java.util.List;

import com.bugzero.rarego.boundedContext.product.domain.Category;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateDto(
	@NotBlank(message = "상품명은 필수입니다.")
	String name,
	@NotNull(message = "카테고리는 필수입니다.")
	Category category,
	@NotNull(message = "상세설명은 필수입니다.")
	String description,
	@Valid
	@NotNull(message = "경매 정보는 필수입니다.")
	ProductAuctionUpdateDto productAuctionUpdateDto,
	@NotEmpty(message = "이미지는 최소 1장 이상 등록해야 합니다.")
	List<@Valid ProductImageUpdateDto> productImageUpdateDtos
) {
}
