package com.bugzero.rarego.boundedContext.auth.app;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthIssueTokenUseCase {
	private final JwtProvider jwtProvider;

	@Value("${jwt.access-token-expire-seconds}")
	private int accessTokenExpireSeconds;

	@Value("${jwt.refresh-token-expire-seconds}")
	private int refreshTokenExpireSeconds;

	private int getTokenExpireSeconds (boolean isAccessToken) {
		return isAccessToken ? accessTokenExpireSeconds : refreshTokenExpireSeconds;
	}


	public String issueToken(String memberPublicId, String role, boolean isAccessToken) {
		validateDto(memberPublicId, role);

		int expireSeconds = getTokenExpireSeconds(isAccessToken);
		if (expireSeconds <= 0) {
			throw new CustomException(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
		}

		try {
			return jwtProvider.issueToken(
				expireSeconds,
				Map.of(
					"publicId", memberPublicId,
					"role", role
				)
			);
		} catch (Exception e) {
			throw new CustomException(ErrorType.JWT_ISSUE_FAILED);
		}
	}

	private void validateDto(String memberPublicId, String role) {
		if (role == null) {
			throw new CustomException(ErrorType.INVALID_INPUT);
		}
		if (memberPublicId == null || memberPublicId.isBlank()) {
			throw new CustomException(ErrorType.INVALID_INPUT);
		}
	}
}
