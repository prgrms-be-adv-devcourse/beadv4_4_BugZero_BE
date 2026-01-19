package com.bugzero.rarego.boundedContext.member.domain;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequestDto(
	@NotBlank
	@Size(max = 50)
	String nickname,

	@Size(max = 255)
	String intro,

	@Size(min = 5, max = 5)
	String zipCode,

	@Size(max = 255)
	String address,

	@Size(max = 255)
	String addressDetail,

	@Size(max = 10)
	String realName,

	@Size(max = 11)
	String contactPhone,

	Set<MemberClearField> clearFields
) {
}