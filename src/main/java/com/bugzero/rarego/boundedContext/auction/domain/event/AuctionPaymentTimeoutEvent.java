package com.bugzero.rarego.boundedContext.auction.domain.event;

public record AuctionPaymentTimeoutEvent(
        Long auctionId,
        Long buyerId,
        Long sellerId,
        int penaltyAmount
) {
}
