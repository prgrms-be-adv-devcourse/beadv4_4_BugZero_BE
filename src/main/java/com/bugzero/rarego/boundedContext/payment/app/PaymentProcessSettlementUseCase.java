package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.event.SettlementFinishedEvent;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.global.event.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessSettlementUseCase {
	private final SettlementRepository settlementRepository;
	private final PaymentSettlementProcessor paymentSettlementProcessor;
	private final EventPublisher eventPublisher;

	@Value("${custom.payment.settlement.holdDays:7}")
	private int settlementHoldDays;

	@Transactional
	public int processSettlements(int limit) {
		// 7일 경과한 정산만 처리
		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(settlementHoldDays);

		List<Settlement> settlements = settlementRepository.findSettlementsForBatch(
			SettlementStatus.READY, cutoffDate, limit);

		if (settlements.isEmpty()) {
			// 정산할 게 없어도, 혹시 이전에 남겨진 수수료가 있다면 처리
			eventPublisher.publish(new SettlementFinishedEvent());
			return 0;
		}

		int successCount = 0;

		for (Settlement settlement : settlements) {
			try {
				if (paymentSettlementProcessor.processSellerDeposit(settlement)) {
					successCount++;
				}
			} catch (Exception e) {
				// TODO: 실패 정산 재시도 처리
				log.error("정산 실패 ID: {} - {}", settlement.getId(), e.getMessage(), e);
				settlement.fail();
			}
		}

		eventPublisher.publish(new SettlementFinishedEvent());

		return successCount;
	}

	private void collectFees() {
		try {
			paymentSettlementProcessor.processFees(1000);
		} catch (Exception e) {
			log.error("시스템 수수료 징수 실패 (데이터는 남아있으므로 다음 배치 때 재시도됨)", e);
		}
	}
}
