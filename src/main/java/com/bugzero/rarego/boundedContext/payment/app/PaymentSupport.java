package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentSupport {
	private final WalletRepository walletRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentMemberRepository paymentMemberRepository;

	public Wallet findWalletByMemberId(Long memberId) {
		return walletRepository.findByMemberId(memberId)
			.orElseThrow(() -> new CustomException(ErrorType.WALLET_NOT_FOUND));
	}

	public Payment findPaymentByOrderId(String orderId) {
		return paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new CustomException(ErrorType.PAYMENT_NOT_FOUND));
	}

	public PaymentMember findMemberById(Long id) {
		return paymentMemberRepository.findById(id)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}

	public Wallet findWalletByMemberIdForUpdate(Long memberId) {
		return walletRepository.findByMemberIdForUpdate(memberId)
				.orElseThrow(() -> new CustomException(ErrorType.WALLET_NOT_FOUND));
	}
}
