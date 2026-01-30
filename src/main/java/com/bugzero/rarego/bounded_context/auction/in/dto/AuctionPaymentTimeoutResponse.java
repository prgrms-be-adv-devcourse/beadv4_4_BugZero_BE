package com.bugzero.rarego.bounded_context.auction.in.dto;

import com.bugzero.rarego.bounded_context.auction.domain.AuctionOrderStatus;

import java.time.LocalDateTime;
import java.util.List;

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