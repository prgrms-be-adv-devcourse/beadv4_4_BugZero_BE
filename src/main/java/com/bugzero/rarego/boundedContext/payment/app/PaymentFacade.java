package com.bugzero.rarego.boundedContext.payment.app;

import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;

@Service
@RequiredArgsConstructor
public class PaymentFacade {

	private final PaymentHoldDepositUseCase paymentHoldDepositUseCase;
	private final PaymentReleaseDepositUseCase paymentReleaseDepositUseCase;
	private final PaymentRequestPaymentUseCase paymentRequestPaymentUseCase;

	/**
	 * 보증금 홀딩
	 */
	public DepositHoldResponseDto holdDeposit(DepositHoldRequestDto request) {
		return paymentHoldDepositUseCase.holdDeposit(request);
	}

	/**
	 * 보증금 환급 (낙찰자 제외)
	 */
	public void releaseDeposits(Long auctionId, Long winnerId) {
		paymentReleaseDepositUseCase.releaseDeposits(auctionId, winnerId);
	}

	/**
	 * 예치금 결제 요청
	 */
	public PaymentRequestResponseDto requestPayment(long memberId, PaymentRequestDto requestDto) {
		return paymentRequestPaymentUseCase.requestPayment(memberId, requestDto);
	}
}
