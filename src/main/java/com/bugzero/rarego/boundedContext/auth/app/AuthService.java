package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.AuthMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final AuthIssueAccessTokenUseCase authIssueAccessTokenUseCase;

	public String issueAccessToken(AuthMember member) {
		return authIssueAccessTokenUseCase.issueToken(member);
	}

	public String issueRefreshToken(AuthMember member, AuthIssueRefreshTokenUseCase authIssueRefreshTokenUseCase) {
		return authIssueRefreshTokenUseCase.issueToken(member);
	}
}

