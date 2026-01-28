package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.out.SettlementFeeRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessSettlementUseCase {
	private final SettlementRepository settlementRepository;
	private final PaymentSettlementProcessor paymentSettlementProcessor;
	private final SettlementFeeRepository settlementFeeRepository;

	@Value("${custom.payment.settlement.holdDays:7}")
	private int settlementHoldDays;

	public int processSettlements(int limit) {
		// 7일 경과한 정산만 처리
		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(settlementHoldDays);
		List<Settlement> settlements = settlementRepository.findSettlementsForBatch(
			SettlementStatus.READY, cutoffDate, PageRequest.of(0, limit));

		if (settlements.isEmpty()) {
			// 정산 대상이 없어도 처리되지 않은 수수료가 있을 수 있으므로 체크
			collectFees();
			return 0;
		}

		int successCount = 0;

		for (Settlement settlement : settlements) {
			try {
				if (paymentSettlementProcessor.processSellerDeposit(settlement.getId())) {
					successCount++;
				}
			} catch (Exception e) {
				log.error("정산 실패 ID: {} - {}", settlement.getId(), e.getMessage(), e);
				markAsFailed(settlement); // 실패 처리 메서드 호출
			}
		}

		collectFees();

		return successCount;
	}

	/**
	 * 미처리 수수료 일괄 처리
	 */
	private void collectFees() {
		try {
			paymentSettlementProcessor.processFees(1000);
		} catch (Exception e) {
			log.error("시스템 수수료 징수 실패", e);
			// 실패해도 SettlementFee 데이터는 남아있으므로 다음 배치에서 재시도됨
		}
	}

	/**
	 * 실패 상태 변경 (안전하게 처리)
	 */
	private void markAsFailed(Settlement settlement) {
		try {
			// TODO: 실패 정산 처리 로직 구현
			paymentSettlementProcessor.fail(settlement.getId());
		} catch (Exception e) {
			log.error("정산 실패 처리 중 에러 발생 ID: {} - {}", settlement.getId(), e.getMessage(), e);
		}
	}
}
