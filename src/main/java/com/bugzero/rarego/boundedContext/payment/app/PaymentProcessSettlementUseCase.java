package com.bugzero.rarego.boundedContext.payment.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessSettlementUseCase {
	private final SettlementRepository settlementRepository;
	private final WalletRepository walletRepository;
	private final PaymentSettlementProcessor paymentSettlementProcessor;

	@Value("${custom.payment.systemMemberId}")
	private Long systemMemberId;

	public int processSettlements(int limit) {
		// 우선 락 없이 목록 조회
		List<Settlement> settlements = settlementRepository.findAllByStatus(SettlementStatus.READY,
			PageRequest.of(0, limit));

		if (settlements.isEmpty()) { // 정산 대상이 없으면 0 반환
			return 0;
		}

		int totalSystemFee = 0;
		int successCount = 0;

		for (Settlement settlement : settlements) {
			try {
				int fee = paymentSettlementProcessor.process(settlement.getId());
				if (fee > 0) {
					totalSystemFee += fee;
					successCount++;
				}
			} catch (Exception e) {
				log.error("정산 실패 ID: {} - {}", settlement.getId(), e.getMessage());

				try {
					paymentSettlementProcessor.fail(settlement.getId()); // 정산 실패 처리
				} catch (Exception e2) {
					log.error("정산 실패 처리 실패 ID: {} - {}", settlement.getId(), e2.getMessage());
				}
			}
		}

		if (totalSystemFee > 0) {
			walletRepository.increaseBalance(systemMemberId, totalSystemFee);
		}

		return successCount;
	}
}
