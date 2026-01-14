package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentStatus;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;
import com.bugzero.rarego.boundedContext.payment.out.TossPaymentsApiClient;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentConfirmPaymentUseCase {
	private final TossPaymentsApiClient tossPaymentsApiClient;
	private final PaymentRepository paymentRepository;
	private final PaymentConfirmFinalizer paymentConfirmFinalizer;

	public PaymentConfirmResponseDto confirmPayment(Long memberId, PaymentConfirmRequestDto requestDto) {
		Payment payment = validateRequest(memberId, requestDto);

		TossPaymentsConfirmResponseDto tossResponse = tossPaymentsApiClient.confirm(requestDto);

		return paymentConfirmFinalizer.finalizePayment(payment, tossResponse);
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
}
