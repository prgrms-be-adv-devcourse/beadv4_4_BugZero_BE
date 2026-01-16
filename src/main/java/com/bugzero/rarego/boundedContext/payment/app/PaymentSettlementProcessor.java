package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentSettlementProcessor {
	private final PaymentSupport paymentSupport;

	@Transactional
	public int process(Long settlementId) {
		// 비관적 락
		Settlement settlement = paymentSupport.findSettlementByIdForUpdate(settlementId);

		if (settlement.getStatus() != SettlementStatus.READY) {
			return 0; // 이미 처리된 건은 스킵
		}

		Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(settlement.getSeller().getId());

		wallet.addBalance(settlement.getSettlementAmount());
		settlement.complete();

		return settlement.getFeeAmount();
	}

	@Transactional
	public void fail(Long settlementId) {
		// 비관적 락
		Settlement settlement = paymentSupport.findSettlementByIdForUpdate(settlementId);

		if (settlement.getStatus() != SettlementStatus.READY) {
			return; // 이미 처리된 건은 스킵
		}

		settlement.fail();
	}
}
