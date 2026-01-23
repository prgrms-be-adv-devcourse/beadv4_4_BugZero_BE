package com.bugzero.rarego.boundedContext.payment.in.dto;

import com.bugzero.rarego.boundedContext.payment.domain.Wallet;

public record WalletResponseDto(
	Long id,
	String publicId,
	int balance,
	int holdingAmount
) {
	public static WalletResponseDto from(Wallet wallet) {
		return new WalletResponseDto(
			wallet.getId(),
			wallet.getMember().getPublicId(),
			wallet.getBalance(),
			wallet.getHoldingAmount()
		);
	}
}
