package com.bugzero.rarego.boundedContext.auction.domain;

public enum AuctionStatus {
	SCHEDULED,      // 예정됨
	IN_PROGRESS,    // 진행 중
	ENDED,          // 종료됨 (시간 만료)
	SOLD,           // 낙찰됨 (구매 확정)
	FAILED          // 유찰됨
}
