package com.bugzero.rarego.bounded_context.payment.in.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.bounded_context.payment.domain.PaymentTransaction;
import com.bugzero.rarego.bounded_context.payment.domain.ReferenceType;
import com.bugzero.rarego.bounded_context.payment.domain.WalletTransactionType;

public record WalletTransactionResponseDto(
	Long id,
	WalletTransactionType type,
	String typeName,
	int balanceDelta,
	int holdingDelta,
	int balance,
	ReferenceType referenceType,
	Long referenceId,
	LocalDateTime createdAt
) {
	public static WalletTransactionResponseDto from(PaymentTransaction transaction) {
		return new WalletTransactionResponseDto(
			transaction.getId(),
			transaction.getTransactionType(),
			transaction.getTransactionType().getDescription(),
			transaction.getBalanceDelta(),
			transaction.getHoldingDelta(),
			transaction.getBalanceAfter(),
			transaction.getReferenceType(),
			transaction.getReferenceId(),
			transaction.getCreatedAt()
		);
	}
}
