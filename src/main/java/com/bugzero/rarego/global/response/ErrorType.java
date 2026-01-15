package com.bugzero.rarego.global.response;

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
	JWT_EXPIRE_SECONDS_INVALID(500, 1504,"토큰 만료 설정이 올바르지 않습니다."),
	JWT_ISSUE_FAILED(500, 1505, "토큰 발급에 실패했습니다."),
	AUTH_UNAUTHORIZED(401, 1506, "인증이 필요합니다."),
	AUTH_FORBIDDEN(403, 1507, "권한이 없습니다."),

    // Auction (2000 ~ 2999)
    AUCTION_NOT_FOUND(404, 2001, "경매를 찾을 수 없습니다."),
    AUCTION_NOT_IN_PROGRESS(400, 2002, "경매가 진행 중인 상태가 아닙니다."),
    AUCTION_ALREADY_HIGHEST_BIDDER(409, 2003, "현재 최고 입찰자이므로 연속 입찰할 수 없습니다."),
    AUCTION_SELLER_CANNOT_BID(403, 2004, "본인의 경매에는 입찰할 수 없습니다."),
    AUCTION_TIME_INVALID(400, 2005, "입찰 가능한 시간이 아닙니다."),
    AUCTION_BID_AMOUNT_TOO_LOW(400, 2006, "입찰 금액이 현재가 또는 시작가보다 낮습니다."),
    BID_NOT_FOUND(404, 2501, "입찰가를 찾을 수 없습니다."),
    AUCTION_NOT_SCHEDULED(400, 2503, "예정된 경매가 아닙니다."),
    AUCTION_SCHEDULE_FAILED(500, 2504, "경매 정산 예약에 실패했습니다."),
    SCHEDULER_CAPACITY_EXCEEDED(503, 2505, "스케줄러 용량이 초과되었습니다."),
    SERVICE_SUBSCRIBER_LIMIT_EXCEEDED(503, 2506, "구독자 수 한도를 초과했습니다."),


    // Product (3000 ~ 3999)

    // Payment (4000 ~ 4999)
    WALLET_NOT_FOUND(404, 4001, "회원의 지갑이 존재하지 않습니다."),
    INSUFFICIENT_BALANCE(400, 4002, "예치금 잔액이 부족합니다."),
    INSUFFICIENT_HOLDING(400, 4003, "환급할 보증금이 부족합니다."),

    PAYMENT_CONFIRM_FAILED(400, 4101, "토스 결제 승인에 실패했습니다."),
    PAYMENT_NOT_FOUND(404, 4102, "존재하지 않는 결제입니다."),
    INVALID_PAYMENT_AMOUNT(400, 4103, "결제 금액이 일치하지 않습니다."),
    ALREADY_PROCESSED_PAYMENT(409, 4104, "이미 처리된 결제입니다."),
    PAYMENT_OWNER_MISMATCH(403, 4105, "해당 결제에 대한 접근 권한이 없습니다."),

	AUCTION_ORDER_NOT_FOUND(404, 4201, "주문 정보를 찾을 수 없습니다."),
	NOT_AUCTION_WINNER(403, 4202, "낙찰자만 결제할 수 있습니다."),
	INVALID_ORDER_STATUS(409, 4203, "결제 가능한 주문 상태가 아닙니다."),
	DEPOSIT_NOT_FOUND(404, 4204, "보증금 정보를 찾을 수 없습니다."),
	ALREADY_USED_DEPOSIT(409, 4205, "이미 사용된 보증금입니다.");

    private final Integer httpStatus;
    private final int code;
    private final String message;
}
