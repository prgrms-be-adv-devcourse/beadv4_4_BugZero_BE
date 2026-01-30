package com.bugzero.rarego.bounded_context.payment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WalletTransactionType {
	TOPUP_DONE("예치금 충전"),
	DEPOSIT_HOLD("보증금 동결"),
	DEPOSIT_RELEASE("보증금 해제"),
	DEPOSIT_USED("보증금 사용"),
	DEPOSIT_FORFEITED("보증금 몰수"),
	AUCTION_PAYMENT("낙찰금 결제"),
	REFUND_DONE("환불"),
	SETTLEMENT_PAID("정산 지급"),
	SETTLEMENT_FEE("정산 수수료");

	private final String description;
}
