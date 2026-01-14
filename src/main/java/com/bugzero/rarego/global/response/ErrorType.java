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

	// Product (3000 ~ 3999)

	// Payment (4000 ~ 4999)
	WALLET_NOT_FOUND(404, 4001, "회원의 지갑이 존재하지 않습니다."),
	INSUFFICIENT_BALANCE(400, 4002, "예치금 잔액이 부족합니다."),
	INSUFFICIENT_HOLDING(400, 4003, "환급할 보증금이 부족합니다."),

	PAYMENT_CONFIRM_FAILED(400, 4101, "토스 결제 승인에 실패했습니다."),
	PAYMENT_NOT_FOUND(400, 4102, "존재하지 않는 결제입니다."),
	INVALID_PAYMENT_AMOUNT(400, 4103, "결제 금액이 일치하지 않습니다."),
	ALREADY_PROCESSED_PAYMENT(400, 4104, "이미 처리된 결제입니다."),
	PAYMENT_OWNER_MISMATCH(403, 4105, "해당 결제에 대한 접근 권한이 없습니다.");

	private final Integer httpStatus;
	private final int code;
	private final String message;
}