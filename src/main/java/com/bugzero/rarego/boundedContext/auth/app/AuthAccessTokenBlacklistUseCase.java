package com.bugzero.rarego.boundedContext.auth.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auth.domain.AccessTokenBlacklist;
import com.bugzero.rarego.boundedContext.auth.out.AccessTokenBlacklistRepository;
import com.bugzero.rarego.global.security.JwtParser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthAccessTokenBlacklistUseCase {
	private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
	private final JwtParser jwtParser;

	// 토큰 원문 노출 방지용 처음 10글자 + 마지막 6글자 마스킹
	private static String mask(String jwt) {
		if (jwt == null)
			return "null";
		int len = jwt.length();
		if (len <= 20)
			return "***";
		return jwt.substring(0, 10) + "..." + jwt.substring(len - 6);
	}

	@Transactional
	public void blacklist(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			return;
		}

		// 이미 만료됐다면 블랙리스트 스킵
		LocalDateTime expiresAt = jwtParser.expiresAt(accessToken);
		LocalDateTime now = LocalDateTime.now();
		if (expiresAt == null || expiresAt.isBefore(now)) {
			return;
		}

		// 이미 블랙리스트에 존재하면 스킵
		if (accessTokenBlacklistRepository.existsByAccessTokenAndExpiresAtAfter(accessToken, now)) {
			return;
		}

		accessTokenBlacklistRepository.save(new AccessTokenBlacklist(accessToken, expiresAt));
	}

	// 블랙리스트 처리 됐는지 확인
	public boolean isBlacklisted(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			return false;
		}
		LocalDateTime now = LocalDateTime.now();
		return accessTokenBlacklistRepository.existsByAccessTokenAndExpiresAtAfter(accessToken, now);
	}

	// 지금보다 이전에 만료된 블랙리스트 일괄 삭제
	// 0개면 0을 필수적으로 리턴하므로 long 사용
	@Transactional
	public long deleteExpired() {
		return accessTokenBlacklistRepository.deleteByExpiresAtBefore(LocalDateTime.now());
	}
}
