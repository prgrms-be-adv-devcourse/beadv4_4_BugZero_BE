package com.bugzero.rarego.bounded_context.auth.domain;

public record AccountDto (
	String providerId,
	String email,
	Provider provider
) {
}
