package com.bugzero.rarego.bounded_context.product.domain.dto;

import lombok.Builder;

@Builder
public record PresignedUrlResponseDto(
	String url,
	String s3Path
) {
}
