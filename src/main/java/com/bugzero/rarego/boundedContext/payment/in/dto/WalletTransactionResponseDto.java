package com.bugzero.rarego.boundedContext.payment.in.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;

public record WalletTransactionResponseDto(
		Long id,
		WalletTransactionType type,
		String typeName,
		int balanceDelta,
		int holdingDelta,
		int balance,
		int holdingAmount,
		ReferenceType referenceType,
		Long referenceId,
		LocalDateTime createdAt) {
	public static WalletTransactionResponseDto from(PaymentTransaction transaction) {
		return new WalletTransactionResponseDto(
				transaction.getId(),
				transaction.getTransactionType(),
				transaction.getTransactionType().getDescription(),
				transaction.getBalanceDelta(),
				transaction.getHoldingDelta(),
				transaction.getBalanceAfter(),
				transaction.getWallet().getHoldingAmount(),
				transaction.getReferenceType(),
				transaction.getReferenceId(),
				transaction.getCreatedAt());
	}
}
