package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.in.dto.WalletResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentGetMyWalletUseCase {
	private final PaymentSupport paymentSupport;

	public WalletResponseDto getMyWallet(String memberPublicId) {
		PaymentMember member = paymentSupport.findMemberByPublicId(memberPublicId);
		return WalletResponseDto.from(paymentSupport.findWalletByMember(member));
	}
}
