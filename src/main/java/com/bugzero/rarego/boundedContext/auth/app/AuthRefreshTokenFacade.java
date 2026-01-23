package com.bugzero.rarego.boundedContext.auth.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.RefreshToken;
import com.bugzero.rarego.boundedContext.auth.domain.TokenPairDto;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.JwtParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRefreshTokenFacade {
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtParser jwtParser;
	private final AuthIssueTokenUseCase authIssueTokenUseCase;
	private final AuthStoreRefreshTokenUseCase authStoreRefreshTokenUseCase;
	private final AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;
	private final AccountRepository accountRepository;

	@Transactional
	public TokenPairDto refresh(String refreshToken, String accessToken) {

		// 1. refresh token 입력 검증
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new CustomException(ErrorType.AUTH_REFRESH_TOKEN_REQUIRED);
		}

		RefreshToken storedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken)
			.orElseThrow(() -> new CustomException(ErrorType.AUTH_REFRESH_TOKEN_INVALID));

		if (storedRefreshToken.isExpired(LocalDateTime.now())) {
			throw new CustomException(ErrorType.AUTH_REFRESH_TOKEN_EXPIRED);
		}

		// 3. JWT 검증
		String refreshPublicId = jwtParser.parseRefreshPublicId(refreshToken);
		if (refreshPublicId == null) {
			throw new CustomException(ErrorType.AUTH_REFRESH_TOKEN_INVALID);
		}
		if (!refreshPublicId.equals(storedRefreshToken.getMemberPublicId())) {
			throw new CustomException(ErrorType.AUTH_REFRESH_TOKEN_OWNER_MISMATCH);
		}

		Account account = accountRepository.findByMemberPublicId(refreshPublicId)
			.orElseThrow(() -> new CustomException(ErrorType.AUTH_REFRESH_TOKEN_INVALID));
		if (account.isDeleted()) {
			refreshTokenRepository.delete(storedRefreshToken);
			throw new CustomException(ErrorType.AUTH_FORBIDDEN);
		}

		refreshTokenRepository.delete(storedRefreshToken);

		String newAccessToken = authIssueTokenUseCase.issueToken(account.getMemberPublicId(), account.getRole().name(),
			true);
		String newRefreshToken = authIssueTokenUseCase.issueToken(account.getMemberPublicId(), account.getRole().name(),
			false);
		authStoreRefreshTokenUseCase.store(account.getMemberPublicId(), newRefreshToken);

		// 블랙리스트 추가
		authAccessTokenBlacklistUseCase.blacklist(accessToken);

		return new TokenPairDto(newAccessToken, newRefreshToken);
	}
}
