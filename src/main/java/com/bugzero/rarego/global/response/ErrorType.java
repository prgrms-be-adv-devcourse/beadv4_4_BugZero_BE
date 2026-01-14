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
    HIGHEST_BID_NOT_FOUND(404, 2501, "최고 입찰가를 찾을 수 없습니다."),
    AUCTION_NOT_IN_PROGRESS(400, 2502, "현재 진행 중인 경매가 아닙니다."),
    AUCTION_NOT_SCHEDULED(400, 2503, "예정된 경매가 아닙니다."),

    // Product (3000 ~ 3999)

    // Payment (4000 ~ 4999)
    WALLET_NOT_FOUND(404, 4001, "회원의 지갑이 존재하지 않습니다."),
    INSUFFICIENT_BALANCE(400, 4002, "예치금 잔액이 부족합니다."),
    INSUFFICIENT_HOLDING(400, 4003, "환급할 보증금이 부족합니다.");

    private final Integer httpStatus;
    private final int code;
    private final String message;
}
