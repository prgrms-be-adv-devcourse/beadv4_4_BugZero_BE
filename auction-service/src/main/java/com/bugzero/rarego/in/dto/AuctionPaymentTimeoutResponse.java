package com.bugzero.rarego.in.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.bugzero.rarego.domain.AuctionOrderStatus;

public record AuctionPaymentTimeoutResponse(
	LocalDateTime requestTime,
	int processedCount,
	List<TimeoutDetail> details
) {
	public record TimeoutDetail(
		Long orderId,
		Long auctionId,
		Long buyerId,
		int penaltyAmount,
		AuctionOrderStatus status
	) {
	}
}
