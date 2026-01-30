package com.bugzero.rarego.bounded_context.member.app;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.bounded_context.member.domain.Member;
import com.bugzero.rarego.bounded_context.member.domain.MemberMeResponseDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberGetMemberUseCase {
	private final MemberSupport memberSupport;

	/**
	 * 본인 정보 조회
	 * @param publicId
	 * @param role
	 * @return MemberMeResponseDto
	 */
	public MemberMeResponseDto getMe(String publicId, String role) {
		Member member = memberSupport.findByPublicId(publicId);
		return MemberMeResponseDto.from(member, role);
	}
}
