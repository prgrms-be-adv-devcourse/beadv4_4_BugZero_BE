package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
	private final PaymentRequestPaymentUseCase paymentRequestPaymentUseCase;

	public PaymentRequestResponseDto requestPayment(long memberId, PaymentRequestDto requestDto) {
		return paymentRequestPaymentUseCase.requestPayment(memberId, requestDto);
	}
}
