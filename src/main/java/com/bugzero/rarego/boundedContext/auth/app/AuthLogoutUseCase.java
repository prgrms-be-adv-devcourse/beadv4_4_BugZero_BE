package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthLogoutUseCase {
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;

	@Transactional
	public void logout(String refreshToken, String accessToken) {
		authAccessTokenBlacklistUseCase.blacklist(accessToken);

		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}

		refreshTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
			.ifPresent(token -> {
				token.revoke();
				refreshTokenRepository.save(token);
			});
	}
}
