package com.bugzero.rarego.bounded_context.payment.in.dto;

import com.bugzero.rarego.bounded_context.payment.domain.Payment;

public record PaymentRequestResponseDto(
	String orderId,
	int amount,
	String customerName,
	String customerEmail
) {
	public static PaymentRequestResponseDto from(Payment payment) {
		return new PaymentRequestResponseDto(
			payment.getOrderId(),
			payment.getAmount(),
			payment.getMember().getNickname(),
			payment.getMember().getEmail()
		);
	}
}
