package com.bugzero.rarego.ai.domain.dto;

import com.bugzero.rarego.ai.domain.type.TemporaryCondition;
import com.bugzero.rarego.shared.product.type.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiInternalPriceRequestDto(
	@NotNull(message = "카테고리는 필수입니다.")
	Category category,
	@NotNull(message = "상품상태 값은 필수입니다.")
	TemporaryCondition condition,
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(max = 100)
	String name,
	@NotNull(message = "상품 설명은 필수입니다.")
	String description
) {
}
