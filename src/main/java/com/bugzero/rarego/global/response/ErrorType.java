package com.bugzero.rarego.global.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorType {
	// Global (9000 ~ 9999)
	INTERNAL_SERVER_ERROR(500, 9000, "서버 오류가 발생했습니다."),
	INVALID_INPUT(400, 9001, "잘못된 입력값입니다."),


	// Member (1000 ~ 1499)
	MEMBER_NOT_FOUND(404, 1001, "존재하지 않는 회원입니다."),

	// Member (1500 ~ 1999)
	// Auth/JWT
	AUTH_MEMBER_REQUIRED(400, 1501, "회원 정보가 필요합니다."),
	AUTH_MEMBER_ID_INVALID(400, 1502,"회원 식별자가 올바르지 않습니다."),
	AUTH_MEMBER_NICKNAME_REQUIRED(400,1503, "닉네임이 필요합니다."),
	JWT_EXPIRE_SECONDS_INVALID(500, 1504,"토큰 만료 설정이 올바르지 않습니다."),
	JWT_ISSUE_FAILED(500, 1505, "토큰 발급에 실패했습니다."),
	AUTH_UNAUTHORIZED(401, 1506, "인증이 필요합니다."),
	AUTH_FORBIDDEN(403, 1507, "권한이 없습니다."),

	// Auction (2000 ~ 2999)

	// Product (3000 ~ 3999)

	// Payment (4000 ~ 4999)
	WALLET_NOT_FOUND(404, 4001, "회원의 지갑이 존재하지 않습니다."),
	INSUFFICIENT_BALANCE(400, 4002, "예치금 잔액이 부족합니다."),
	INSUFFICIENT_HOLDING(400, 4003, "환급할 보증금이 부족합니다.");

	private final Integer httpStatus;
	private final int code;
	private final String message;
}
