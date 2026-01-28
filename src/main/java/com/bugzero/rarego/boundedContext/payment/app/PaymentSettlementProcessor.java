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

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean processSellerDeposit(Long settlementId) {
		Settlement settlement = paymentSupport.findSettlementByIdForUpdate(settlementId);

		if (settlement.getStatus() != SettlementStatus.READY) {
			return false;
		}

		// 1. 판매자 지갑 입금
		Wallet sellerWallet = paymentSupport.findWalletByMemberIdForUpdate(settlement.getSeller().getId());
		sellerWallet.addBalance(settlement.getSettlementAmount());

		saveSettlementTransaction(
			sellerWallet,
			WalletTransactionType.SETTLEMENT_PAID,
			settlement.getSettlementAmount(),
			settlement.getId()
		);

		// 2. 정산 완료 처리
		settlement.complete();

		// 3. 수수료 테이블에 저장
		SettlementFee fee = SettlementFee.builder()
			.settlement(settlement)
			.feeAmount(settlement.getFeeAmount())
			.build();
		settlementFeeRepository.save(fee);

		return true;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processSystemDeposit(int totalFeeAmount, List<SettlementFee> fees) {
		// 1. 시스템 지갑 입금
		Wallet systemWallet = paymentSupport.findWalletByMemberIdForUpdate(systemMemberId);
		systemWallet.addBalance(totalFeeAmount);

		saveSettlementTransaction(
			systemWallet,
			WalletTransactionType.SETTLEMENT_FEE,
			totalFeeAmount,
			0L
		); // 합산 건이므로 referenceId는 고민

		// 2. 처리된 수수료 데이터 삭제
		settlementFeeRepository.deleteAllInBatch(fees);
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
