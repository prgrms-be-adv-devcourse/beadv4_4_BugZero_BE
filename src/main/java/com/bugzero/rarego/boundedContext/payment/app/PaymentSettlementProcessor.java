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
	public boolean processSellerDeposit(Long settlementId) {
		Settlement settlement = paymentSupport.findSettlementByIdForUpdate(settlementId);

		if (settlement.getStatus() != SettlementStatus.READY) {
			return false;
		}

		Wallet sellerWallet = paymentSupport.findWalletByMemberIdForUpdate(settlement.getSeller().getId());
		sellerWallet.addBalance(settlement.getSettlementAmount());

		saveSettlementTransaction(
			sellerWallet,
			WalletTransactionType.SETTLEMENT_PAID,
			settlement.getSettlementAmount(),
			settlement.getId()
		);

		settlement.complete();

		return true;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processSystemDeposit(int totalFeeAmount) {
		Wallet systemWallet = paymentSupport.findWalletByMemberIdForUpdate(systemMemberId);
		systemWallet.addBalance(totalFeeAmount);

		saveSettlementTransaction(
			systemWallet,
			WalletTransactionType.SETTLEMENT_FEE,
			totalFeeAmount,
			0L
		); // 합산 건이므로 referenceId는 고민
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
