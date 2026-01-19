package com.bugzero.rarego.boundedContext.member.domain;

import java.time.LocalDateTime;

public record MemberMeResponseDto(

	// 계정 provider(KAKAO, GOOGLE, NAVER) 제외 => member 모듈에 없음
	// int balance // => 이후에 wallet과 연결

	String publicId,
	String role,
	String email,
	String nickname,
	String intro,
	String address,
	String addressDetail,
	String zipCode,
	String contactPhoneMasked,
	String realNameMasked,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static MemberMeResponseDto from(Member member, String role) {
		return new MemberMeResponseDto(
			member.getPublicId(),
			role,
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
