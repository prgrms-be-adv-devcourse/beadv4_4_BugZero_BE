package com.bugzero.rarego.boundedContext.auth.app;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;
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


	public String issueToken(TokenIssueDto tokenIssueDto, boolean isAccessToken) {
		validateDto(tokenIssueDto);

		int expireSeconds = getTokenExpireSeconds(isAccessToken);
		if (expireSeconds <= 0) {
			throw new CustomException(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
		}

		try {
			return jwtProvider.issueToken(
				expireSeconds,
				Map.of(
					"publicId", tokenIssueDto.memberPublicId(),
					"role", tokenIssueDto.role()
				)
			);
		} catch (Exception e) {
			throw new CustomException(ErrorType.JWT_ISSUE_FAILED);
		}
	}

	private void validateDto(TokenIssueDto tokenIssueDto) {
		if (tokenIssueDto.role() == null) {
			throw new CustomException(ErrorType.INVALID_INPUT);
		}
		if (tokenIssueDto.memberPublicId().isBlank()) {
			throw new CustomException(ErrorType.INVALID_INPUT);
		}
	}
}
