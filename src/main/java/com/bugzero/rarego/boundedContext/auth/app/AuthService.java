package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.AuthMember;
import com.bugzero.rarego.boundedContext.auth.out.AuthMemberRepository;
import com.bugzero.rarego.shared.member.domain.MemberRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final AuthIssueAccessTokenUseCase authIssueAccessTokenUseCase;
	private final AuthMemberRepository authMemberRepository;

	public String issueAccessToken(AuthMember member) {
		return authIssueAccessTokenUseCase.issueToken(member);
	}

	public String issueRefreshToken(AuthMember member, AuthIssueRefreshTokenUseCase authIssueRefreshTokenUseCase) {
		return authIssueRefreshTokenUseCase.issueToken(member);
	}

	// 테스트용 메서드, 1~5까지 입력 가능 + 새로운 멤버 생성
	public String testIssueAccessToken(Long memberId) {
		AuthMember member = authMemberRepository.findById(memberId)
			.orElseGet(() -> authMemberRepository.save(
				AuthMember.builder()
					.id(memberId)
					.nickname("새로운테스트멤버" + memberId)
					.role(MemberRole.USER)
					.build()
			));
		return authIssueAccessTokenUseCase.issueToken(member);
	}
}

