package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.domain.TokenPairDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthFacade {
	private final AuthIssueTokenUseCase authIssueTokenUseCase;
	private final AuthLoginAccountFacade authLoginAccountFacade;
	private final AuthStoreRefreshTokenUseCase authStoreRefreshTokenUseCase;

	// 테스트용 accessToken 발급
	public String issueAccessToken(String providerId, String role) {
		return authIssueTokenUseCase.issueToken(providerId, role, true);
	}

	// 로그인, 회원가입 통합
	public TokenPairDto login(String providerId, String email, Provider provider) {
		Account account = authLoginAccountFacade.loginOrSignup(providerId, email, provider);
		String accessToken = authIssueTokenUseCase.issueToken(account.getMemberPublicId(), account.getRole().name(), true);
		String refreshToken = authIssueTokenUseCase.issueToken(account.getMemberPublicId(), account.getRole().name(), false);
		authStoreRefreshTokenUseCase.store(account.getMemberPublicId(), refreshToken);
		return new TokenPairDto(accessToken, refreshToken);
	}
}
