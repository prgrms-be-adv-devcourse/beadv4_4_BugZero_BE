package com.bugzero.rarego.app;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.domain.PaymentMember;
import com.bugzero.rarego.domain.SettlementStatus;
import com.bugzero.rarego.domain.WalletTransactionType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.in.dto.AuctionFinalPaymentResponseDto;
import com.bugzero.rarego.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.in.dto.PaymentRequestDto;
import com.bugzero.rarego.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.in.dto.RefundResponseDto;
import com.bugzero.rarego.in.dto.WalletResponseDto;
import com.bugzero.rarego.in.dto.WalletTransactionResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
import com.bugzero.rarego.shared.payment.dto.SettlementResponseDto;

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
	private final PaymentGetSettlementsUseCase paymentGetSettlementsUseCase;
	private final PaymentSyncMemberUseCase paymentSyncMemberUseCase;
	private final PaymentGetMyWalletUseCase paymentGetMyWalletUseCase;
	private final PaymentWithdrawUseCase paymentWithdrawUseCase;
	private final PaymentAuctionExpiringSoonUseCase paymentAuctionExpiringSoonUseCase;

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
	 * 보증금 단건 환급 (입찰 실패 시 보상 트랜잭션용)
	 */
	public void releaseDeposit(Long auctionId, String memberPublicId) {
		paymentReleaseDepositUseCase.releaseDeposit(auctionId, memberPublicId);
	}

	/**
	 * 예치금 결제 요청
	 */
	public PaymentRequestResponseDto requestPayment(String memberPublicId, PaymentRequestDto requestDto) {
		return paymentRequestPaymentUseCase.requestPayment(memberPublicId, requestDto);
	}

	/**
	 * 예치금 결제 승인
	 */
	public PaymentConfirmResponseDto confirmPayment(String memberPublicId, PaymentConfirmRequestDto requestDto) {
		return paymentConfirmPaymentUseCase.confirmPayment(memberPublicId, requestDto);
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
	public AuctionFinalPaymentResponseDto auctionFinalPayment(String memberPublicId, Long auctionId,
		AuctionFinalPaymentRequestDto requestDto) {
		return paymentAuctionFinalUseCase.finalPayment(memberPublicId, auctionId, requestDto);
	}

	/**
	 * 지갑 거래 내역 조회
	 */
	public PagedResponseDto<WalletTransactionResponseDto> getWalletTransactions(String memberPublicId, int page,
		int size,
		WalletTransactionType transactionType, LocalDate from, LocalDate to) {
		return paymentGetWalletTransactionsUseCase.getWalletTransactions(memberPublicId, page, size, transactionType,
			from, to);

	}

	/**
	 * 정산 내역 조회
	 */
	public PagedResponseDto<SettlementResponseDto> getSettlements(String memberPublicId, int page, int size,
		SettlementStatus status, LocalDate from, LocalDate to) {
		return paymentGetSettlementsUseCase.getSettlements(memberPublicId, page, size, status, from, to);
	}

	/**
	 * 환불 처리
	 */
	public RefundResponseDto processRefund(Long auctionId) {
		return paymentRefundUseCase.processRefund(auctionId);
	}

	/**
	 * PaymentMember 동기화
	 */
	public PaymentMember syncMember(MemberDto member) {
		return paymentSyncMemberUseCase.syncMember(member);
	}

	public boolean hasProcessingOrders(String publicId) {
		return paymentWithdrawUseCase.hasProcessingOrders(publicId);
	}

	/**
	 * 내 지갑 조회
	 */
	public WalletResponseDto getMyWallet(String memberPublicId) {
		return paymentGetMyWalletUseCase.getMyWallet(memberPublicId);
	}

	/**
	 * 결제 마감 임박 이벤트 발행
	 */
	public void publishExpiringSoonEvent(AuctionOrderDto order, LocalDateTime expiredAt) {
		paymentAuctionExpiringSoonUseCase.publishExpiringSoonEvent(order, expiredAt);
	}
}
