package com.bugzero.rarego.boundedContext.payment.in.dto;

public record TossPaymentsConfirmResponseDto(
	String orderId,
	String paymentKey,
	Integer totalAmount
) {
}
