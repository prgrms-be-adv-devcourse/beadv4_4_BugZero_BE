package com.bugzero.rarego.in.dto;

public record PaymentConfirmResponseDto(
	String orderId,
	int amount,
	int balance
) {
	public static PaymentConfirmResponseDto of(TossPaymentsResponseDto tossResponse, int balance) {
		return new PaymentConfirmResponseDto(
			tossResponse.orderId(),
			tossResponse.totalAmount(),
			balance
		);
	}
}
