package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.AuthMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final AuthIssueTokenUseCase authIssueTokenUseCase;

	public String issueAuthToken(AuthMember member) {
		return authIssueTokenUseCase.issueToken(member);
	}
}

