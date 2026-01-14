package com.bugzero.rarego.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorType {
	// server
	INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다."),

	// member
	MEMBER_NOT_FOUND(404, "존재하지 않는 회원입니다."),

	// auction

	// product

	// payment
	WALLET_NOT_FOUND(404, "회원의 지갑이 존재하지 않습니다."),
	INSUFFICIENT_BALANCE(400, "예치금 잔액이 부족합니다."),
	INSUFFICIENT_HOLDING(400, "환급할 보증금이 부족합니다.");

	private final Integer httpStatus;
	private final String message;
}
