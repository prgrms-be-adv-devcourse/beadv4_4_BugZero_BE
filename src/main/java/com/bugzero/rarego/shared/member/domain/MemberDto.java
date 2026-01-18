package com.bugzero.rarego.shared.member.domain;

import java.time.LocalDateTime;

public record MemberDto(
	Long id,
	String publicId,
	String email,
	String nickname,
	String intro,
	String address,
	String address_detail,
	String zipCode,
	String contactPhone,
	String realNameEnc,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	boolean deleted
) {
	public static MemberDto from(SourceMember member) {
		return new MemberDto(
			member.getId(),
			member.getPublicId(),
			member.getEmail(),
			member.getNickname(),
			member.getIntro(),
			member.getAddress(),
			member.getAddress_detail(),
			member.getZipCode(),
			member.getContactPhone(),
			member.getRealNameEnc(),
			member.getCreatedAt(),
			member.getUpdatedAt(),
			member.isDeleted()
		);
	}
}
