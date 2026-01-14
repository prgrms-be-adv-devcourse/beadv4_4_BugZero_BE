package com.bugzero.rarego.global.security;

import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtParser {
	private final String secret;

	public JwtParser(@Value("${jwt.secret}") String secret) {
		this.secret = secret;
	}

	public boolean isValid(String jwtStr) {
		SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

		try {
			Jwts
				.parser()
				.verifyWith(secretKey)
				.build()
				.parse(jwtStr);
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public Map<String, Object> payload(String jwtStr) {
		SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

		try {
			return (Map<String, Object>) Jwts
				.parser()
				.verifyWith(secretKey)
				.build()
				.parse(jwtStr)
				.getPayload();
		} catch (Exception e) {
			return null;
		}
	}

	public MemberPrincipal parsePrincipal(String jwtStr) {
		Map<String, Object> payload = payload(jwtStr);
		if (payload == null)
			return null;

		Long id = toLong(payload.get("id"));
		String nickname = (String) payload.get("nickname");
		return new MemberPrincipal(id, nickname);
	}

	private static Long toLong(Object value) {
		if (value == null)
			return null;
		if (value instanceof Long longValue)
			return longValue;
		if (value instanceof Integer intValue)
			return intValue.longValue();
		if (value instanceof Number numberValue)
			return numberValue.longValue();
		if (value instanceof String stringValue) {
			try {
				return Long.parseLong(stringValue);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
