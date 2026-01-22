package com.bugzero.rarego.boundedContext.auth.app;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.RefreshToken;
import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthStoreRefreshTokenUseCase {
	private final RefreshTokenRepository refreshTokenRepository;

	@Value("${jwt.refresh-token-expire-seconds}")
	private int refreshTokenExpireSeconds;

	@PostConstruct
	void validate() {
		if (refreshTokenExpireSeconds <= 0) {
			throw new CustomException(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
		}
	}

	public void store(String memberPublicId, String refreshToken) {
		if (memberPublicId == null || memberPublicId.isBlank()) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}

		LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpireSeconds);
		refreshTokenRepository.save(new RefreshToken(memberPublicId, refreshToken, expiresAt, false));
	}
}
