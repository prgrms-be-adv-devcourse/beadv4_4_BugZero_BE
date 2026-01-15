package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentConfirmFinalizer {
	private final PaymentRepository paymentRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final PaymentSupport paymentSupport;

	@Transactional
	public PaymentConfirmResponseDto finalizePayment(Payment payment,
		TossPaymentsConfirmResponseDto tossResponse) {
		// 결제 완료 처리
		payment.complete(tossResponse.paymentKey());

		paymentRepository.save(payment); // payment는 영속성 컨텍스트와 연결이 끊긴 상태라 명시적으로 저장

		Wallet wallet = paymentSupport.findWalletByMemberId(payment.getMember().getId());

		// 지갑 잔액 증가
		wallet.addBalance(payment.getAmount());

		// PaymentTransaction 기록
		PaymentTransaction paymentTransaction = PaymentTransaction.builder()
			.member(payment.getMember())
			.wallet(wallet)
			.transactionType(WalletTransactionType.TOPUP_DONE)
			.balanceDelta(payment.getAmount())
			.holdingDelta(0)
			.balanceAfter(wallet.getBalance())
			.referenceType(ReferenceType.PAYMENT)
			.referenceId(payment.getId())
			.build();

		paymentTransactionRepository.save(paymentTransaction);

		return PaymentConfirmResponseDto.of(tossResponse, wallet.getBalance());
	}
}
