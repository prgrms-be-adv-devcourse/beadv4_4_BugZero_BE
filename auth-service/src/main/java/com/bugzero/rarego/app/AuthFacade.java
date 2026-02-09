package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.domain.Account;
import com.bugzero.rarego.domain.Provider;
import com.bugzero.rarego.domain.TokenPairDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthFacade {
	private final AuthIssueTokenUseCase authIssueTokenUseCase;
	private final AuthLoginAccountFacade authLoginAccountFacade;
	private final AuthStoreRefreshTokenUseCase authStoreRefreshTokenUseCase;
	private final AuthRefreshTokenFacade authRefreshTokenFacade;
	private final AuthLogoutAccountUseCase authLogoutAccountUseCase;
	private final AuthWithdrawAccountUseCase authWithdrawAccountUseCase;
	private final AuthPromoteSellerUseCase authPromoteSellerUseCase;

	// 테스트용 accessToken 발급
	public String issueAccessToken(String providerId, String role) {
		return authIssueTokenUseCase.issueToken(providerId, role, true);
	}

	// 로그인, 회원가입 통합
	public TokenPairDto login(String providerId, String email, Provider provider) {
		Account account = authLoginAccountFacade.loginOrSignup(providerId, email, provider);
		String accessToken = authIssueTokenUseCase.issueToken(account.getMemberPublicId(), account.getRole().name(),
			true);
		String refreshToken = authIssueTokenUseCase.issueToken(account.getMemberPublicId(), account.getRole().name(),
			false);
		authStoreRefreshTokenUseCase.store(account.getMemberPublicId(), refreshToken);
		return new TokenPairDto(accessToken, refreshToken);
	}

	public TokenPairDto refresh(String refreshToken, String accessToken) {
		return authRefreshTokenFacade.refresh(refreshToken, accessToken);
	}

	public void logout(String refreshToken, String accessToken) {
		authLogoutAccountUseCase.logout(refreshToken, accessToken);
	}

	public void withdraw(String accessToken) {
		authWithdrawAccountUseCase.withdraw(accessToken);
	}

	public void promoteSeller(String accessToken) {
		authPromoteSellerUseCase.promoteSeller(accessToken);
	}
}
