package com.bugzero.rarego.boundedContext.auction.domain;

public enum AuctionOrderStatus {
	// 결제 진행중
	PROCESSING,
	// 결제 성공
	SUCCESS,
	// 결제 실패
	FAILED
}
