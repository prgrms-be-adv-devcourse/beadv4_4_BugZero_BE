package com.bugzero.rarego.global.security;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtParser {
	private final SecretKey jwtSecretKey;

	// 인코딩 고정
	public JwtParser(@Value("${jwt.secret}") String jwtSecretKey) {
		this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
	}

	// 토큰 원문 노출 방지용 처음 10글자 + 마지막 6글자 마스킹
	private static String mask(String jwt) {
		if (jwt == null)
			return "null";
		int len = jwt.length();
		if (len <= 20)
			return "***";
		return jwt.substring(0, 10) + "..." + jwt.substring(len - 6);
	}

	public boolean isValid(String jwtStr) {
		try {
			Jwts.parser()
				.verifyWith(jwtSecretKey)
				.build()
				.parse(jwtStr);

			return true;

		} catch (ExpiredJwtException e) {
			log.debug("JWT expired. token={}, msg={}", mask(jwtStr), e.getMessage());
			return false;

		} catch (SecurityException e) {
			// 서명 불일치/검증 실패 성격
			log.debug("JWT signature/security error. token={}, msg={}", mask(jwtStr), e.getMessage());
			return false;

		} catch (JwtException e) {
			// 형식 오류, claim 오류 등 JJWT 계열 전반
			log.debug("JWT invalid. token={}, ex={}, msg={}",
				mask(jwtStr), e.getClass().getSimpleName(), e.getMessage());
			return false;

		} catch (IllegalArgumentException e) {
			// null/blank 등
			log.debug("JWT illegal argument. token={}, msg={}", mask(jwtStr), e.getMessage());
			return false;

		} catch (Exception e) {
			// 정말 예상 못한 케이스
			log.warn("JWT unexpected error. token={}, ex={}", mask(jwtStr), e.getClass().getName(), e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> payload(String jwtStr) {
		try {
			Object payload = Jwts.parser()
				.verifyWith(jwtSecretKey)
				.build()
				.parse(jwtStr)
				.getPayload();

			if (!(payload instanceof Map<?, ?> map)) {
				log.debug("JWT payload type is not Map. payloadType={}",
					payload == null ? "null" : payload.getClass().getName());
				return null;
			}

			return (Map<String, Object>)map;

		} catch (ExpiredJwtException e) {
			log.debug("JWT expired while reading payload: {}", e.getMessage());
			return null;

		} catch (SecurityException e) {
			log.debug("JWT signature/security error while reading payload: {}", e.getMessage());
			return null;

		} catch (JwtException e) {
			log.debug("JWT invalid while reading payload. ex={}, msg={}",
				e.getClass().getSimpleName(), e.getMessage());
			return null;

		} catch (IllegalArgumentException e) {
			log.debug("JWT illegal argument while reading payload: {}", e.getMessage());
			return null;

		} catch (Exception e) {
			log.warn("JWT unexpected error while reading payload. ex={}", e.getClass().getName(), e);
			return null;
		}
	}

	public MemberPrincipal parsePrincipal(String jwtStr) {
		Map<String, Object> payload = payload(jwtStr);
		if (payload == null)
			return null;

		Long id = toLong(payload.get("id"));
		String role = toRoleString(payload.get("role"));
		if (id == null || role == null || role.isBlank()) {
			return null;
		}

		return new MemberPrincipal(id, role);
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

	private static String toRoleString(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String stringValue) {
			if (stringValue.startsWith("ROLE_"))
				return stringValue.substring("ROLE_".length());
			return stringValue;
		}
		return value.toString();
	}
}
