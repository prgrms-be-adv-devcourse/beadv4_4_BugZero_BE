package com.bugzero.rarego.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
	AUCTION_OUTBID("입찰가 추월"),
	BOOKMARK_AUCTION_STARTED("관심 경매 시작"),
	AUCTION_WON("경매 낙찰"),
	AUCTION_PAYMENT_COMPLETED("경매 결제 완료"),
	AUCTION_PAYMENT_EXPIRING_SOON("낙찰 결제 임박"),
	SETTLEMENT_COMPLETED("정산 완료");

	private final String description;
}
