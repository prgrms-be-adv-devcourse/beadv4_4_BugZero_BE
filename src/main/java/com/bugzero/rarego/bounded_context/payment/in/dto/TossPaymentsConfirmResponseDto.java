package com.bugzero.rarego.bounded_context.payment.in.dto;

public record TossPaymentsConfirmResponseDto(
	String orderId,
	String paymentKey,
	Integer totalAmount
) {
}
