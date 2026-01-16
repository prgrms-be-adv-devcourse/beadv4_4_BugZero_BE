package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

		// 판매자 정산 이력 저장
		saveTransaction(wallet, WalletTransactionType.SETTLEMENT_PAID, settlement.getSettlementAmount(),
			settlement.getId());

		// 시스템 수수료 입금
		if (settlement.getFeeAmount() > 0) {
			Wallet systemWallet = paymentSupport.findWalletByMemberIdForUpdate(systemMemberId);

			systemWallet.addBalance(settlement.getFeeAmount());

			// 수수료 처리 이력 저장
			saveTransaction(systemWallet, WalletTransactionType.SETTLEMENT_FEE,
				settlement.getFeeAmount(), settlement.getId());
		}

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

	private void saveTransaction(Wallet wallet, WalletTransactionType type, int amount, Long refId) {
		PaymentTransaction transaction = PaymentTransaction.builder()
			.wallet(wallet)
			.member(wallet.getMember())
			.transactionType(type)
			.balanceDelta(amount)
			.holdingDelta(0)
			.balanceAfter(wallet.getBalance()) // 변경 후 잔액 스냅샷
			.referenceType(ReferenceType.SETTLEMENT)
			.referenceId(refId)
			.build();
		paymentTransactionRepository.save(transaction);
	}
}
