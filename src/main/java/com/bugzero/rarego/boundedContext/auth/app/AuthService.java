package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final AuthIssueTokenUseCase authIssueTokenUseCase;


	public String issueAccessToken(TokenIssueDto tokenIssueDto) {
		return authIssueTokenUseCase.issueToken(tokenIssueDto, true);
	}

	public String issueRefreshToken(TokenIssueDto tokenIssueDto) {
		return  authIssueTokenUseCase.issueToken(tokenIssueDto, false);
	}
}

