package com.bugzero.rarego.in;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.bugzero.rarego.app.AuctionOutboxProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionOutboxRetryScheduler {

	private final AuctionOutboxProcessor auctionOutboxProcessor;

	/**
	 * 미처리된 아웃박스 재시도
	 * - 1분마다 실행
	 * - PENDING 상태의 아웃박스 조회
	 * - 재시도 횟수 < 3인 것만 처리
	 */
	@Scheduled(fixedDelayString = "${auction.outbox.retry-delay-ms:60000}")
	@SchedulerLock(name = "auctionOutboxRetryLock", lockAtMostFor = "5m", lockAtLeastFor = "1m")
	public void retry() {
		try {
			int success = auctionOutboxProcessor.retryPending();
			if (success > 0) {
				log.info("경매 아웃박스 재시도 완료: 성공 {}건", success);
			}
		} catch (Exception e) {
			log.error("경매 아웃박스 재시도 중 심각한 오류 발생", e);
		}
	}
}
