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
	@Value("${jwt.auth-token-expire-seconds}")
	private int authTokenExpireSeconds;

	public String issueToken(AuthMember member) {
		validateMember(member);
		if (authTokenExpireSeconds <= 0) {
			throw new CustomException(ErrorType.JWT_EXPIRE_SECONDS_INVALID);
		}

		try {
			return jwtProvider.issueToken(
				authTokenExpireSeconds,
				Map.of(
					"id", member.getId(),
					"nickname", member.getNickname()
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
