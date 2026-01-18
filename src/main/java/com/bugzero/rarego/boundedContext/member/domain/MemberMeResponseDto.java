package com.bugzero.rarego.boundedContext.member.domain;

import java.time.LocalDateTime;

import com.bugzero.rarego.global.util.MaskingUtils;

public record MemberMeResponseDto(
	String publicId,
	String role,
	// 계정 provider(KAKAO, GOOGLE, NAVER) 제외 => member 모듈에 없음
	String email,
	String nickname,
	String intro,
	// int balance // => 이후에 wallet과 연결
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
			MaskingUtils.maskPhone(member.getContactPhone()),
			MaskingUtils.maskRealName(member.getRealName()),
			member.getCreatedAt(),
			member.getUpdatedAt()
		);
	}

}
