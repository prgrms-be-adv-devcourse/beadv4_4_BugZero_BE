package com.bugzero.rarego.boundedContext.auth.domain;

public record TokenPairDto(
	String accessToken,
	String refreshToken
) {
}
