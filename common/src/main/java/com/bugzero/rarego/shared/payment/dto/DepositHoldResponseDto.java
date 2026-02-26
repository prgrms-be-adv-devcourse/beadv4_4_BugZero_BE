package com.bugzero.rarego.shared.payment.dto;

import java.time.LocalDateTime;

public record DepositHoldResponseDto(
	Long depositId,
	Long auctionId,
	int amount,
	String status,
	LocalDateTime createdAt,
	boolean holdApplied) {
}
