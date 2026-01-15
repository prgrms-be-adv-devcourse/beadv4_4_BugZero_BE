package com.bugzero.rarego.boundedContext.product.domain.dto;

import lombok.Builder;

@Builder
public record PresignedUrlResponseDto(
	String url,
	String s3Path
) {
}
