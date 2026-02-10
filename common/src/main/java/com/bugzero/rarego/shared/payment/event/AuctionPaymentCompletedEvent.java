package com.bugzero.rarego.shared.payment.event;

public record AuctionPaymentCompletedEvent(
	Long orderId,
	Long auctionId,
	Long sellerId,
	Long buyerId,
	int amount
) {
}
