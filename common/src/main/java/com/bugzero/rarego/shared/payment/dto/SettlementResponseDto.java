package com.bugzero.rarego.shared.payment.dto;

import java.time.LocalDateTime;

public record SettlementResponseDto(
	Long id,
	Long auctionId,
	Long sellerId,
	int salesAmount,
	int feeAmount,
	int settlementAmount,
	String status,
	LocalDateTime createdAt
) {
}
