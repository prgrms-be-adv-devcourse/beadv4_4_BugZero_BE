package com.bugzero.rarego.bounded_context.auth.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TokenIssueDto(
	@NotNull @Positive String memberPublicId,
	@NotBlank String role
) { }
