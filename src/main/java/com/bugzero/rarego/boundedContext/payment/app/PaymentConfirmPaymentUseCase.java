package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentStatus;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.TossPaymentsApiClient;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentConfirmPaymentUseCase {
	private final TossPaymentsApiClient tossPaymentsApiClient;
	private final PaymentRepository paymentRepository;
	private final WalletRepository walletRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;

	public PaymentConfirmResponseDto confirmPayment(Long memberId, PaymentConfirmRequestDto requestDto) {
		Payment payment = validateRequest(memberId, requestDto);

		TossPaymentsConfirmResponseDto tossResponse = tossPaymentsApiClient.confirm(requestDto);

		return processPaymentFinalization(payment, tossResponse);
	}

	private Payment validateRequest(Long memberId, PaymentConfirmRequestDto requestDto) {
		Payment payment = paymentRepository.findByOrderId(requestDto.orderId())
			.orElseThrow(() -> new CustomException(ErrorType.PAYMENT_NOT_FOUND));

		// 내 주문이 맞는지 확인
		if (!payment.getMember().getId().equals(memberId)) {
			throw new CustomException(ErrorType.PAYMENT_OWNER_MISMATCH);
		}

		// 금액 위변조 확인
		if (payment.getAmount() != requestDto.amount()) {
			throw new CustomException(ErrorType.INVALID_PAYMENT_AMOUNT);
		}

		// 이미 처리된 주문인지 확인
		if (payment.getStatus() != PaymentStatus.PENDING) {
			throw new CustomException(ErrorType.ALREADY_PROCESSED_PAYMENT);
		}

		return payment;
	}

	@Transactional
	protected PaymentConfirmResponseDto processPaymentFinalization(Payment payment,
		TossPaymentsConfirmResponseDto tossResponse) {
		// 결제 완료 처리
		payment.complete(tossResponse.paymentKey());

		Wallet wallet = walletRepository.findByMemberId(payment.getMember().getId())
			.orElseThrow(() -> new CustomException(ErrorType.WALLET_NOT_FOUND));

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
