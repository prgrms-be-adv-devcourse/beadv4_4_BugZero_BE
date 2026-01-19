package com.bugzero.rarego.shared.payment.event;

public record PaymentTimeoutEvent(
        Long auctionId,
        Long buyerId,
        Long sellerId,
        int penaltyAmount) {
}
