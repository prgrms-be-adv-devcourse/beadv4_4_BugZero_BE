package com.bugzero.rarego.bounded_context.payment.app;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.bounded_context.payment.domain.Payment;
import com.bugzero.rarego.bounded_context.payment.domain.PaymentTransaction;
import com.bugzero.rarego.bounded_context.payment.domain.ReferenceType;
import com.bugzero.rarego.bounded_context.payment.domain.Wallet;
import com.bugzero.rarego.bounded_context.payment.domain.WalletTransactionType;
import com.bugzero.rarego.bounded_context.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.bounded_context.payment.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.bounded_context.payment.out.PaymentRepository;
import com.bugzero.rarego.bounded_context.payment.out.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentConfirmFinalizer {
	private final PaymentRepository paymentRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final PaymentSupport paymentSupport;

	@Transactional
	public PaymentConfirmResponseDto finalizePayment(Payment payment, TossPaymentsConfirmResponseDto tossResponse) {
		payment.complete(tossResponse.paymentKey()); // 결제 완료 처리
		paymentRepository.save(payment); // payment는 준영속 상태이므로 명시적 저장

		// 지갑 조회(비관적 락)
		Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(payment.getMember().getId());

		wallet.addBalance(payment.getAmount());

		PaymentTransaction transaction = PaymentTransaction.builder()
			.member(payment.getMember())
			.wallet(wallet)
			.transactionType(WalletTransactionType.TOPUP_DONE)
			.balanceDelta(payment.getAmount())
			.holdingDelta(0)
			.balanceAfter(wallet.getBalance())
			.referenceType(ReferenceType.PAYMENT)
			.referenceId(payment.getId())
			.build();

		paymentTransactionRepository.save(transaction);

		return PaymentConfirmResponseDto.of(tossResponse, wallet.getBalance());
	}
}
