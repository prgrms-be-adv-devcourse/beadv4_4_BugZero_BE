package com.bugzero.rarego.boundedContext.auth.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TokenIssueDto(
	@NotNull @Positive Long memberId,
	@NotBlank String role
) { }
