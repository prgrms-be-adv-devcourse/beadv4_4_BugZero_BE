package com.bugzero.rarego.in;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.bugzero.rarego.app.PaymentOutboxProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxRetryScheduler {
	private final PaymentOutboxProcessor paymentOutboxProcessor;

	@Scheduled(fixedDelayString = "${payment.outbox.retry-delay-ms:60000}")
	@SchedulerLock(name = "outboxRetryLock", lockAtMostFor = "5m", lockAtLeastFor = "5m")
	public void retry() {
		int success = paymentOutboxProcessor.retryPending();
		if (success > 0) {
			log.info("아웃박스 재시도 완료: 성공 {}건", success);
		}
	}
}
