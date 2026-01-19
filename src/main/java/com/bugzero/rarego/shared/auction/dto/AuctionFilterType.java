package com.bugzero.rarego.shared.auction.dto;

public enum AuctionFilterType {
	ALL,
	// SCHEDULED, IN_PROGRESS
	ONGOING,
	// ENDED (정상 거래 중/완료)
	COMPLETED,
	// FAILED, ENDED인데 결제 취소됨
	ACTION_REQUIRED
}
