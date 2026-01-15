package com.bugzero.rarego.boundedContext.auth.app;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.AuthMember;
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


	public String issueToken(AuthMember member, boolean isAccessToken) {
		validateMember(member);

		int expireSeconds = getTokenExpireSeconds(isAccessToken);
		if (expireSeconds <= 0) {
			throw new CustomException(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
		}

		try {
			return jwtProvider.issueToken(
				expireSeconds,
				Map.of(
					"id", member.getId(),
					"nickname", member.getNickname(),
					"role", member.getRole().name()
				)
			);
		} catch (Exception e) {
			throw new CustomException(ErrorType.JWT_ISSUE_FAILED);
		}
	}

	private void validateMember(AuthMember member) {
		if (member == null) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}
		if (member.getId() == null || member.getId() <= 0) {
			throw new CustomException(ErrorType.AUTH_MEMBER_ID_INVALID);
		}
		if (member.getNickname() == null || member.getNickname().isBlank()) {
			throw new CustomException(ErrorType.AUTH_MEMBER_NICKNAME_REQUIRED);
		}
	}
}
