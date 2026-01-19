package com.bugzero.rarego.boundedContext.auth.domain;

public record AccountDto (
	String providerId,
	String email,
	Provider provider
) {
}
