package com.bugzero.rarego.in;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.PaymentFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {
	PaymentFacade paymentFacade;

	@Scheduled(cron = "0 0/10 * * * *") // 10분마다 실행
	public void scheduleRecovery() {
		paymentFacade.recoverPendingPayments();
	}
}
