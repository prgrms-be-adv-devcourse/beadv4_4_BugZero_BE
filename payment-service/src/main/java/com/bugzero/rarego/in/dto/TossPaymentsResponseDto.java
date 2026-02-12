package com.bugzero.rarego.in.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentsResponseDto(
	String orderId,
	String paymentKey,
	String status,
	Integer totalAmount
) {
}
