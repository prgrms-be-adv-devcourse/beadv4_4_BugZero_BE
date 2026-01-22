package com.bugzero.rarego.boundedContext.payment.app;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.in.dto.*;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
    private final PaymentWithdrawUseCase paymentWithdrawUseCase;

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
    @Transactional
    public PaymentMember syncMember(MemberDto member) {
        return paymentSyncMemberUseCase.syncMember(member);
    }

    public boolean hasProcessingOrders(String publicId) {
        return paymentWithdrawUseCase.hasProcessingOrders(publicId);
    }
}
