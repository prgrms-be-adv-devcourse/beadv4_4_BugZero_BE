package com.bugzero.rarego.shared.payment.event;

import java.time.LocalDateTime;

public record AuctionPaymentExpiringSoonEvent(
	Long orderId,
	Long auctionId,
	Long buyerId,
	Long sellerId,
	int amount,
	LocalDateTime expiredAt
) {
}
