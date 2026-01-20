package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
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
		payment.complete(tossResponse.paymentKey()); // 결제 완료 처리
		paymentRepository.save(payment); // payment는 준영속 상태이므로 명시적 저장

		// 지갑 조회(비관적 락)
		Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(payment.getMember().getId());

		// 예치금 충전 및 거래 내역 생성
		PaymentTransaction transaction = wallet.topUp(payment.getAmount(), ReferenceType.PAYMENT, payment.getId());

		paymentTransactionRepository.save(transaction);

		return PaymentConfirmResponseDto.of(tossResponse, wallet.getBalance());
	}
}
