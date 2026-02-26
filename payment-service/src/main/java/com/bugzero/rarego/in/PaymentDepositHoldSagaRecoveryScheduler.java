package com.bugzero.rarego.in;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.PaymentDepositHoldSagaRecoveryUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.saga.deposit-hold-recovery.enabled", havingValue = "true")
public class PaymentDepositHoldSagaRecoveryScheduler {
	private final PaymentDepositHoldSagaRecoveryUseCase recoveryUseCase;

	@Scheduled(cron = "${payment.saga.deposit-hold-recovery.cron:0 */5 * * * *}")
	public void recoverExpiredDepositHolds() {
		try {
			recoveryUseCase.recoverExpiredDepositHolds();
		} catch (Exception e) {
			log.error("보증금 홀드 Saga 복구 스케줄러 실행 실패: {}", e.getMessage());
		}
	}
}
