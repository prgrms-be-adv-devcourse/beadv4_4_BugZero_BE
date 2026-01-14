package com.bugzero.rarego.global.security;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtProvider {
	public static String createToken(String secret, int expireSeconds, Map<String, Object> body) {
		ClaimsBuilder claimsBuilder = Jwts.claims();

		for (Map.Entry<String, Object> entry : body.entrySet()) {
			claimsBuilder.add(entry.getKey(), entry.getValue());
		}

		Claims claims = claimsBuilder.build();

		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + 86400L * expireSeconds);

		Key secretKey = Keys.hmacShaKeyFor(secret.getBytes());

		return Jwts.builder()
			.claims(claims)
			.issuedAt(issuedAt)
			.expiration(expiration)
			.signWith(secretKey)
			.compact();
	}
}
