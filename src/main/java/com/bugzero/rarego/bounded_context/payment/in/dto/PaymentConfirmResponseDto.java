package com.bugzero.rarego.bounded_context.payment.in.dto;

public record PaymentConfirmResponseDto(
	String orderId,
	int amount,
	int balance
) {
	public static PaymentConfirmResponseDto of(TossPaymentsConfirmResponseDto tossResponse, int balance) {
		return new PaymentConfirmResponseDto(
			tossResponse.orderId(),
			tossResponse.totalAmount(),
			balance
		);
	}
}
