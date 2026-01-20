package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentSettlementProcessor {
	private final PaymentSupport paymentSupport;
	private final PaymentTransactionRepository paymentTransactionRepository;

	@Value("${custom.payment.systemMemberId}")
	private Long systemMemberId;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void process(Long settlementId) {
		// 비관적 락
		Settlement settlement = paymentSupport.findSettlementByIdForUpdate(settlementId);

		if (settlement.getStatus() != SettlementStatus.READY) {
			return; // 이미 처리된 건은 스킵
		}

		// 판매자 정산금 입금
		Wallet sellerWallet = paymentSupport.findWalletByMemberIdForUpdate(settlement.getSeller().getId());
		sellerWallet.addBalance(settlement.getSettlementAmount());

		saveSettlementTransaction(
			sellerWallet,
			WalletTransactionType.SETTLEMENT_PAID,
			settlement.getSettlementAmount(),
			settlement.getId()
		);

		// 시스템 수수료 입금
		if (settlement.getFeeAmount() > 0) {
			Wallet systemWallet = paymentSupport.findWalletByMemberIdForUpdate(systemMemberId);
			systemWallet.addBalance(settlement.getFeeAmount());

			saveSettlementTransaction(
				systemWallet,
				WalletTransactionType.SETTLEMENT_FEE,
				settlement.getFeeAmount(),
				settlement.getId()
			);
		}

		settlement.complete();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void fail(Long settlementId) {
		// 비관적 락
		Settlement settlement = paymentSupport.findSettlementByIdForUpdate(settlementId);

		if (settlement.getStatus() != SettlementStatus.READY) {
			return; // 이미 처리된 건은 스킵
		}

		settlement.fail();
	}

	private void saveSettlementTransaction(Wallet wallet, WalletTransactionType type, int amount, Long settlementId) {
		PaymentTransaction transaction = PaymentTransaction.builder()
			.wallet(wallet)
			.member(wallet.getMember())
			.transactionType(type)
			.balanceDelta(amount)
			.holdingDelta(0)
			.balanceAfter(wallet.getBalance())
			.referenceType(ReferenceType.SETTLEMENT)
			.referenceId(settlementId)
			.build();

		paymentTransactionRepository.save(transaction);
	}
}
