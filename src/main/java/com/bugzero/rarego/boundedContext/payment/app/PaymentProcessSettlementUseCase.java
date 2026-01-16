package com.bugzero.rarego.boundedContext.payment.app;

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

	@Value("${custom.payment.systemMemberId}")
	private Long systemMemberId;

	public int processSettlements(int limit) {
		// 락 없이 정산 목록 조회
		List<Settlement> settlements = settlementRepository.findAllByStatus(SettlementStatus.READY,
			PageRequest.of(0, limit));

		if (settlements.isEmpty())
			return 0;

		int totalSystemFee = 0;
		int successCount = 0;

		for (Settlement settlement : settlements) {
			int fee = processOne(settlement);

			if (fee > 0) {
				totalSystemFee += fee;
				successCount++;
			}
		}

		depositSystemFee(totalSystemFee);

		return successCount;
	}

	/**
	 * 단건 처리 및 예외 핸들링을 전담
	 * 성공 시 수수료 반환, 실패 시 0 반환
	 */
	private int processOne(Settlement settlement) {
		try {
			return paymentSettlementProcessor.process(settlement.getId());
		} catch (Exception e) {
			log.error("정산 실패 ID: {} - {}", settlement.getId(), e.getMessage());
			markAsFailed(settlement); // 실패 처리 메서드 호출
			return 0;
		}
	}

	/**
	 * 실패 상태 변경 (안전하게 처리)
	 */
	private void markAsFailed(Settlement settlement) {
		try {
			paymentSettlementProcessor.fail(settlement.getId());
		} catch (Exception e) {
			log.error("정산 실패 처리(FAILED 마킹) 실패 ID: {} - {}", settlement.getId(), e.getMessage());
		}
	}

	/**
	 * 시스템 수수료 입금
	 */
	private void depositSystemFee(int amount) {
		if (amount <= 0)
			return;

		try {
			paymentSettlementProcessor.depositSystemFee(systemMemberId, amount);
		} catch (Exception e) {
			log.error("시스템 지갑 입금 실패 - 금액: {}, 원인: {}", amount, e.getMessage());
		}
	}
}
