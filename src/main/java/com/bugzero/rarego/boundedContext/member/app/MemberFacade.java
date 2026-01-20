package com.bugzero.rarego.boundedContext.member.app;

import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.member.domain.MemberMeResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberFacade {
	private final MemberJoinMemberUseCase memberJoinMemberUseCase;
	private final MemberGetMemberUseCase memberGetMemberUseCase;

	@Transactional
	public MemberJoinResponseDto join(String email) {
		return memberJoinMemberUseCase.join(email);
	}

	@Transactional(readOnly = true)
	public MemberMeResponseDto getMe(String publicId, String role) {
		return memberGetMemberUseCase.getMe(publicId, role);
	}
}
