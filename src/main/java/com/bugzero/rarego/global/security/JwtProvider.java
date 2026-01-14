package com.bugzero.rarego.global.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {
	private final String secret;

	public JwtProvider(@Value("${jwt.secret}") String secret) {
		this.secret = secret;
	}

	public String issueToken(int expireSeconds, Map<String, Object> body) {
		ClaimsBuilder claimsBuilder = Jwts.claims();

		for (Map.Entry<String, Object> entry : body.entrySet()) {
			claimsBuilder.add(entry.getKey(), entry.getValue());
		}

		Claims claims = claimsBuilder.build();

		Date issuedAt = new Date();
		// 만료 시간 = 발급 시간 + 만료 기간(초)
		Date expiration = new Date(issuedAt.getTime() + 1000L * expireSeconds);

		Key secretKey = Keys.hmacShaKeyFor(secret.getBytes());

		return Jwts.builder()
			.claims(claims)
			.issuedAt(issuedAt)
			.expiration(expiration)
			.signWith(secretKey)
			.compact();
	}
}
