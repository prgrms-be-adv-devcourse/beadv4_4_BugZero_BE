package com.bugzero.rarego.boundedContext.product.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignedUrlRequestDto(
	@NotBlank(message = "파일명은 필수입니다.")
	String fileName,

	@NotBlank(message = "컨텐츠 타입은 필수입니다.")
	@Pattern(regexp = "^image/(jpeg|png|webp|gif)$", message = "허용되지 않은 이미지 형식입니다.")
	String contentType
) {
}
