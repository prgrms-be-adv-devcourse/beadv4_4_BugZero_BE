package com.bugzero.rarego.boundedContext.auction.domain;

public enum AuctionStatus {
	// 예정됨
	SCHEDULED,
	// 진행 중
	IN_PROGRESS,
	// 종료됨 (시간 만료)
	ENDED,
}
