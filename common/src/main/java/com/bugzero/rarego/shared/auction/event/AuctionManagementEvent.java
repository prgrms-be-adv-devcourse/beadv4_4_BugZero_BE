package com.bugzero.rarego.shared.auction.event;

import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;

public record AuctionManagementEvent(
	AuctionProductEventType eventType,
	String requestId,
	Long productId,
	String publicId,
	String payload
) {
}
