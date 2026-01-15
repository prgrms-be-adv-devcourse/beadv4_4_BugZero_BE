package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
	private final PaymentHoldDepositUseCase paymentHoldDepositUseCase;
	private final PaymentRequestPaymentUseCase paymentRequestPaymentUseCase;
	private final PaymentConfirmPaymentUseCase paymentConfirmPaymentUseCase;

	/**
	 * 보증금 홀딩
	 */
	public DepositHoldResponseDto holdDeposit(DepositHoldRequestDto request) {
		return paymentHoldDepositUseCase.holdDeposit(request);
	}

	/**
	 * 예치금 결제 요청
	 */
	public PaymentRequestResponseDto requestPayment(long memberId, PaymentRequestDto requestDto) {
		return paymentRequestPaymentUseCase.requestPayment(memberId, requestDto);
	}

	/**
	 * 예치금 결제 승인
	 */
	public PaymentConfirmResponseDto confirmPayment(Long memberId, PaymentConfirmRequestDto requestDto) {
		return paymentConfirmPaymentUseCase.confirmPayment(memberId, requestDto);
	}
}
