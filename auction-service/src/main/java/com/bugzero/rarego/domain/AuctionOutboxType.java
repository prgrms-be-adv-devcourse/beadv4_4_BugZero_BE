package com.bugzero.rarego.domain;

public enum AuctionOutboxType {
	AUCTION_ENDED,    // 낙찰 완료
	AUCTION_RELISTED, // 경매 재생성 (유찰/미결제)
	AUCTION_STARTED   // 경매 시작
}
