package com.bugzero.rarego.global.security;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtProviderTest {
	private static final String SECRET = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890";

	@Test
	@DisplayName("issueToken은 id, role 클레임을 포함한 JWT를 발급한다.")
	void issueTokenIncludesClaims() {
		JwtProvider jwtProvider = new JwtProvider(SECRET);

		String jwt = jwtProvider.issueToken(
			60 * 60,
			Map.of("id", 1L, "role", AuthRole.USER.name())
		);

		assertThat(jwt).isNotBlank();

		SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		Claims claims = Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(jwt)
			.getPayload();

		assertThat(((Number) claims.get("id")).longValue()).isEqualTo(1L);
		assertThat(claims.get("role")).isEqualTo(AuthRole.USER.name());
	}
}
