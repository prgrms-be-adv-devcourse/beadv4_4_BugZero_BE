package com.bugzero.rarego.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorType {
	// Global (9000 ~ 9999)
	INTERNAL_SERVER_ERROR(500, 9000, "서버 오류가 발생했습니다."),
	INVALID_INPUT(400, 9001, "잘못된 입력값입니다."),
	
	// Member (1000 ~ 1999)
	MEMBER_NOT_FOUND(404, 1001, "존재하지 않는 회원입니다."),

	// Auction (2000 ~ 2999)
	AUCTION_NOT_FOUND(404, 2001, "경매를 찾을 수 없습니다."),
	AUCTION_NOT_IN_PROGRESS(400, 2002, "경매가 진행 중인 상태가 아닙니다."),
	AUCTION_ALREADY_HIGHEST_BIDDER(409, 2003, "현재 최고 입찰자이므로 연속 입찰할 수 없습니다."),
	AUCTION_SELLER_CANNOT_BID(403, 2004, "본인의 경매에는 입찰할 수 없습니다."),
	AUCTION_TIME_INVALID(400, 2005, "입찰 가능한 시간이 아닙니다."),
	AUCTION_BID_AMOUNT_TOO_LOW(400, 2006, "입찰 금액이 현재가 또는 시작가보다 낮습니다."),

	// Product (3000 ~ 3999)

	// Payment (4000 ~ 4999)
	WALLET_NOT_FOUND(404, 4001, "회원의 지갑이 존재하지 않습니다."),
	INSUFFICIENT_BALANCE(400, 4002, "예치금 잔액이 부족합니다."),
	INSUFFICIENT_HOLDING(400, 4003, "환급할 보증금이 부족합니다.");

	private final Integer httpStatus;
	private final int code;
	private final String message;
}