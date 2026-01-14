package com.bugzero.rarego.global.security;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.bugzero.rarego.boundedContext.auth.app.AuthService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JwtProviderTest.TestConfig.class)
@ActiveProfiles("test")
public class JwtProviderTest {
	@Configuration
	static class TestConfig {
		@Bean
		AuthService authService() {
			return new AuthService();
		}
	}
	@Autowired
	private AuthService authService;
	private int expireSeconds = 60 * 60 * 24 * 365;
	private String secret = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890";

	@Test
	@DisplayName("authTokenService 서비스가 존재한다.")
	void t1() {
		assertThat(authService).isNotNull();
	}

	@Test
	@DisplayName("jjwt 최신 방식으로 JWT 생성, {id=1L, nickname=\"친절한 옥수수\"}")
	void t2() {
		long expireMillis = 1000L * expireSeconds;

		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + expireMillis);

		String jwt = Jwts.builder()
			.claims(Map.of("id", 1L, "nickname", "친절한 옥수수"))
			.issuedAt(issuedAt)
			.expiration(expiration)
			.signWith(secretKey)
			.compact();

		assertThat(jwt).isNotBlank();

		System.out.println("jwt = " + jwt);
	}

	@Test
	@DisplayName("jwtProvider.createToken 통해서 JWT 생성, {id=1L, nickname=\"친절한 옥수수\"}")
	void t3() {
		String jwt = JwtProvider.createToken(
			secret,
			expireSeconds,
			Map.of("id", 1L, "nickname", "친절한 옥수수")
		);

		assertThat(jwt).isNotBlank();

		System.out.println("jwt = " + jwt);
	}
}
