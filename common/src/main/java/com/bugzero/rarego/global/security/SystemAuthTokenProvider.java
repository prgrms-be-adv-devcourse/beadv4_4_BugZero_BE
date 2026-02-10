package com.bugzero.rarego.global.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SystemAuthTokenProvider {
	private final int accessTokenExpirationSeconds;
	private final JwtProvider jwtProvider;

	public SystemAuthTokenProvider(
		@Value("${jwt.access-token-expire-seconds:3600}") int accessTokenExpirationSeconds,
		JwtProvider jwtProvider) {
		this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
		this.jwtProvider = jwtProvider;
	}

	public String getSystemAccessToken() {
		return jwtProvider.issueToken(
			accessTokenExpirationSeconds,
			Map.of(
			"publicId", "00000000-0000-0000-0000-000000000001",
			"role", "SYSTEM"
			)
		);
	}
}
