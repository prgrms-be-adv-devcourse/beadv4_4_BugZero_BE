package com.bugzero.rarego.boundedContext.payment.in.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;

public record SettlementResponseDto(
	Long id,
	Long auctionId,
	Long sellerId,
	int salesAmount,
	int feeAmount,
	int settlementAmount,
	SettlementStatus status,
	LocalDateTime createdAt
) {
	public static SettlementResponseDto from(Settlement settlement) {
		return new SettlementResponseDto(
			settlement.getId(),
			settlement.getAuctionId(),
			settlement.getSeller().getId(),
			settlement.getSalesAmount(),
			settlement.getFeeAmount(),
			settlement.getSettlementAmount(),
			settlement.getStatus(),
			settlement.getCreatedAt()
		);
	}
}
