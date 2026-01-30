package com.bugzero.rarego.bounded_context.member.domain;

import java.time.LocalDateTime;

public record MemberUpdateResponseDto(
	String publicId,
	String email,
	String nickname,
	String intro,
	String address,
	String addressDetail,
	String zipCode,
	String contactPhone,
	String realName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static MemberUpdateResponseDto from(Member member) {
		return new MemberUpdateResponseDto(
			member.getPublicId(),
			member.getEmail(),
			member.getNickname(),
			member.getIntro(),
			member.getAddress(),
			member.getAddressDetail(),
			member.getZipCode(),
			member.getContactPhone(),
			member.getRealName(),
			member.getCreatedAt(),
			member.getUpdatedAt()
		);
	}
}
