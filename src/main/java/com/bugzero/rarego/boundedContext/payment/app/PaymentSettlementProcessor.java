package com.bugzero.rarego.boundedContext.payment.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementFee;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementFeeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentSettlementProcessor {
	private final PaymentSupport paymentSupport;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final SettlementFeeRepository settlementFeeRepository;

	@Value("${custom.payment.systemMemberId}")
	private Long systemMemberId;

	public boolean processSellerDeposit(Settlement settlement) {
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

		SettlementFee fee = SettlementFee.builder()
			.settlement(settlement)
			.feeAmount(settlement.getFeeAmount())
			.build();
		settlementFeeRepository.save(fee);

		return true;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processFees(int limit) {
		// 1. [SKIP LOCKED] 다른 스레드가 처리 중인 건 건너뛰고 조회
		List<SettlementFee> fees = settlementFeeRepository.findAllForBatch(limit);

		if (fees.isEmpty()) {
			return;
		}

		// 2. 금액 합산
		int totalFeeAmount = fees.stream()
			.mapToInt(SettlementFee::getFeeAmount)
			.sum();

		// 3. 시스템 지갑 입금
		if (totalFeeAmount > 0) {
			Wallet systemWallet = paymentSupport.findWalletByMemberIdForUpdate(systemMemberId);
			systemWallet.addBalance(totalFeeAmount);

			saveSettlementTransaction(
				systemWallet,
				WalletTransactionType.SETTLEMENT_FEE,
				totalFeeAmount,
				0L // 여러 건 합산이므로 ID 0
			);
		}

		// 4. 처리된 수수료 데이터 삭제 (Queue 비우기)
		settlementFeeRepository.deleteAllInBatch(fees);
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
