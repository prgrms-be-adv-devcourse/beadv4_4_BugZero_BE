package com.bugzero.rarego.boundedContext.auth.in;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.auth.app.AuthAccessTokenBlacklistUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklistCleanupScheduler {
	private final AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;

	@Scheduled(cron = "0 0 3 * * *")
	public void cleanupExpiredAccessTokens() {
		long deletedCount = 0;

		log.info("[Auth] 만료된 블랙리스트 토큰 제거 시작");
		try {
			deletedCount = authAccessTokenBlacklistUseCase.deleteExpired();
		} catch (Exception e) {
			log.error("[Auth] 만료된 블랙리스트 토큰 제거 실패", e);
		}

		if (deletedCount > 0) {
			log.info("[Auth] 만료된 블랙리스트 토큰 제거 완료: deleted={}", deletedCount);
		} else {
			log.info("[Auth] 삭제 대상 토큰 없음");
		}
		// 0개라도 성공 처리
	}
}
