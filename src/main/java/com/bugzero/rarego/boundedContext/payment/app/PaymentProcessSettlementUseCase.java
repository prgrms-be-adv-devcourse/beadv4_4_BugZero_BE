package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessSettlementUseCase {
	private final SettlementRepository settlementRepository;
	private final PaymentSettlementProcessor paymentSettlementProcessor;

	@Value("${custom.payment.settlement.holdDays:7}")
	private int settlementHoldDays;

	public int processSettlements(int limit) {
		// 7일 경과한 정산만 처리
		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(settlementHoldDays);
		List<Settlement> settlements = settlementRepository.findSettlementsForBatch(
			SettlementStatus.READY, cutoffDate, PageRequest.of(0, limit));

		if (settlements.isEmpty()) {
			return 0;
		}

		int successCount = 0;

		for (Settlement settlement : settlements) {
			if (processOne(settlement)) {
				successCount++;
			}
		}

		return successCount;
	}

	/**
	 * 단건 처리 및 예외 핸들링을 전담
	 * 성공 여부 반환
	 */
	private boolean processOne(Settlement settlement) {
		try {
			paymentSettlementProcessor.process(settlement.getId());
			return true;
		} catch (Exception e) {
			log.error("정산 실패 ID: {} - {}", settlement.getId(), e.getMessage(), e);
			markAsFailed(settlement); // 실패 처리 메서드 호출
			return false;
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
