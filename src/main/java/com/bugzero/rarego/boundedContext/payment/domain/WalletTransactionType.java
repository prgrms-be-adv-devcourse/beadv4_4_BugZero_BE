package com.bugzero.rarego.boundedContext.payment.domain;

public enum WalletTransactionType {
	TOPUP_DONE,
	TOPUP_FAILED,
	DEPOSIT_HOLD,
	DEPOSIT_RELEASE,
	DEPOSIT_USED,
	DEPOSIT_FORFEITED,
	AUCTION_PAYMENT,
	REFUND_DONE,
	SETTLEMENT_PAID,
	SETTLEMENT_FEE
}
