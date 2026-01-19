package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.RefundResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.WalletTransactionResponseDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
	private final PaymentHoldDepositUseCase paymentHoldDepositUseCase;
	private final PaymentReleaseDepositUseCase paymentReleaseDepositUseCase;
	private final PaymentRequestPaymentUseCase paymentRequestPaymentUseCase;
	private final PaymentConfirmPaymentUseCase paymentConfirmPaymentUseCase;
	private final PaymentProcessSettlementUseCase paymentProcessSettlementUseCase;
	private final PaymentAuctionFinalUseCase paymentAuctionFinalUseCase;
	private final PaymentGetWalletTransactionsUseCase paymentGetWalletTransactionsUseCase;
	private final PaymentRefundUseCase paymentRefundUseCase;

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

	/**
	 * 예치금 결제 승인
	 */
	public PaymentConfirmResponseDto confirmPayment(Long memberId, PaymentConfirmRequestDto requestDto) {
		return paymentConfirmPaymentUseCase.confirmPayment(memberId, requestDto);
	}

	/**
	 * 정산 처리
	 */
	public int processSettlements(int chunkSize) {
		return paymentProcessSettlementUseCase.processSettlements(chunkSize);
	}

	/**
	 * 낙찰 결제 (최종 결제)
	 */
	public AuctionFinalPaymentResponseDto auctionFinalPayment(Long memberId, Long auctionId,
		AuctionFinalPaymentRequestDto requestDto) {
		return paymentAuctionFinalUseCase.finalPayment(memberId, auctionId, requestDto);
	}

	/**
	 * 지갑 거래 내역 조회
	 */
	public PagedResponseDto<WalletTransactionResponseDto> getWalletTransactions(Long memberId, int page, int size,
		WalletTransactionType transactionType) {
		return paymentGetWalletTransactionsUseCase.getWalletTransactions(memberId, page, size, transactionType);

	}

	/**
	 * 환불 처리
	 */
	public RefundResponseDto processRefund(Long auctionId) {
		return paymentRefundUseCase.processRefund(auctionId);
	}
}
