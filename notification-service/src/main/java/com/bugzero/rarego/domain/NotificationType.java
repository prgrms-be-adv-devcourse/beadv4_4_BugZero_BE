package com.bugzero.rarego.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
	AUCTION_OUTBID("입찰가 추월", "/auction/%d"),
	BOOKMARK_AUCTION_STARTED("관심 경매 시작", "/auction/%d"),
	AUCTION_WON("경매 낙찰", "/mypage/orders"),
	AUCTION_PAYMENT_COMPLETED("경매 결제 완료", "/mypage/sales"),
	AUCTION_PAYMENT_EXPIRING_SOON("낙찰 결제 임박", "/mypage/orders"),
	SETTLEMENT_COMPLETED("정산 완료", "/mypage/wallet");

	private final String description;
	private final String url;

	public String makeUrl(Long referenceId) {
		if (!url.contains("%")) {
			return url;
		}
		return url.formatted(referenceId);
	}
}
