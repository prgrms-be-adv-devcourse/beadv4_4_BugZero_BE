package com.bugzero.rarego.bounded_context.member.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.bounded_context.member.domain.MemberMeResponseDto;
import com.bugzero.rarego.bounded_context.member.domain.MemberUpdateIdentityRequestDto;
import com.bugzero.rarego.bounded_context.member.domain.MemberUpdateRequestDto;
import com.bugzero.rarego.bounded_context.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberFacade {
	private final MemberJoinMemberUseCase memberJoinMemberUseCase;
	private final MemberGetMemberUseCase memberGetMemberUseCase;
	private final MemberUpdateMemberUseCase memberUpdateMemberUseCase;
	private final MemberUpdateIdentityUseCase memberUpdateIdentityUseCase;
	private final MemberPromoteSellerUseCase memberPromoteSellerUseCase;

	@Transactional
	public MemberJoinResponseDto join(String email) {
		return memberJoinMemberUseCase.join(email);
	}

	@Transactional(readOnly = true)
	public MemberMeResponseDto getMe(String publicId, String role) {
		return memberGetMemberUseCase.getMe(publicId, role);
	}

	@Transactional
	public MemberUpdateResponseDto updateMe(String publicId, String role, MemberUpdateRequestDto requestDto) {
		return memberUpdateMemberUseCase.updateMe(publicId, role, requestDto);
	}

	@Transactional
	public MemberUpdateResponseDto updateIdentity(String publicId, MemberUpdateIdentityRequestDto requestDto) {
		return memberUpdateIdentityUseCase.updateIdentity(publicId, requestDto);
	}
	@Transactional
	public void promoteSeller(String publicId, String role) {
		memberPromoteSellerUseCase.promoteSeller(publicId, role);
	}
}
