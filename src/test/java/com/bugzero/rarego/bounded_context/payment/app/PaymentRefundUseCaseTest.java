package com.bugzero.rarego.bounded_context.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.bounded_context.payment.domain.PaymentMember;
import com.bugzero.rarego.bounded_context.payment.domain.PaymentTransaction;
import com.bugzero.rarego.bounded_context.payment.domain.ReferenceType;
import com.bugzero.rarego.bounded_context.payment.domain.Settlement;
import com.bugzero.rarego.bounded_context.payment.domain.SettlementStatus;
import com.bugzero.rarego.bounded_context.payment.domain.Wallet;
import com.bugzero.rarego.bounded_context.payment.domain.WalletTransactionType;
import com.bugzero.rarego.bounded_context.payment.in.dto.RefundResponseDto;
import com.bugzero.rarego.bounded_context.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.bounded_context.payment.out.SettlementRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;

@ExtendWith(MockitoExtension.class)
class PaymentRefundUseCaseTest {

    @InjectMocks
    private PaymentRefundUseCase paymentRefundUseCase;

    @Mock
    private AuctionOrderPort auctionOrderPort;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private PaymentSupport paymentSupport;

    private static final Long AUCTION_ID = 100L;
    private static final Long BIDDER_ID = 1L;
    private static final Long SELLER_ID = 2L;
    private static final int FINAL_PRICE = 100000;

    @Test
    @DisplayName("성공: 환불 처리 시 지갑 잔액 증가 및 상태 변경")
    void processRefund_Success() {
        // given
        AuctionOrderDto order = new AuctionOrderDto(
                1L, AUCTION_ID, SELLER_ID, BIDDER_ID, FINAL_PRICE, "SUCCESS", LocalDateTime.now());

        PaymentMember buyer = createMockMember(BIDDER_ID);
        Wallet wallet = Wallet.builder().balance(50000).holdingAmount(0).build();
        Settlement settlement = Settlement.create(AUCTION_ID, createMockMember(SELLER_ID), FINAL_PRICE);
        // Settlement 상태는 READY (환불 가능)

        given(auctionOrderPort.refundOrderWithLock(AUCTION_ID)).willReturn(order);
        given(settlementRepository.findByAuctionIdForUpdate(AUCTION_ID)).willReturn(Optional.of(settlement));
        given(paymentSupport.findWalletByMemberIdForUpdate(BIDDER_ID)).willReturn(wallet);
        given(paymentSupport.findMemberById(BIDDER_ID)).willReturn(buyer);
        given(transactionRepository.save(any(PaymentTransaction.class))).willAnswer(invocation -> {
            PaymentTransaction pt = invocation.getArgument(0);
            return pt; // Return the same object mimicking save
        });

        // when
        RefundResponseDto response = paymentRefundUseCase.processRefund(AUCTION_ID);

        // then
        assertThat(wallet.getBalance()).isEqualTo(150000); // 50000 + 100000
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.FAILED);
        assertThat(response.refundAmount()).isEqualTo(FINAL_PRICE);

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        PaymentTransaction savedTx = transactionCaptor.getValue();
        assertThat(savedTx.getTransactionType()).isEqualTo(WalletTransactionType.REFUND_DONE);
        assertThat(savedTx.getBalanceDelta()).isEqualTo(FINAL_PRICE);
        assertThat(savedTx.getReferenceType()).isEqualTo(ReferenceType.AUCTION_ORDER);
    }

    @Test
    @DisplayName("실패: 정산 정보가 없으면 환불 실패")
    void processRefund_Fail_SettlementNotFound() {
        // given
        given(auctionOrderPort.refundOrderWithLock(AUCTION_ID))
                .willReturn(new AuctionOrderDto(1L, AUCTION_ID, SELLER_ID, BIDDER_ID, FINAL_PRICE, "SUCCESS",
                        LocalDateTime.now()));
        given(settlementRepository.findByAuctionIdForUpdate(AUCTION_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentRefundUseCase.processRefund(AUCTION_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.SETTLEMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 주문 상태가 SUCCESS가 아님")
    void processRefund_Fail_InvalidOrderStatus() {
        // given
        given(auctionOrderPort.refundOrderWithLock(AUCTION_ID))
                .willThrow(new CustomException(ErrorType.INVALID_ORDER_STATUS));

        // when & then
        assertThatThrownBy(() -> paymentRefundUseCase.processRefund(AUCTION_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("실패: 정산이 이미 완료됨 (DONE 상태)")
    void processRefund_Fail_SettlementAlreadyCompleted() {
        // given
        AuctionOrderDto order = new AuctionOrderDto(
                1L, AUCTION_ID, SELLER_ID, BIDDER_ID, FINAL_PRICE, "SUCCESS", LocalDateTime.now());

        Settlement settlement = Settlement.create(AUCTION_ID, createMockMember(SELLER_ID), FINAL_PRICE);
        settlement.complete(); // DONE 상태 - 환불 불가

        given(auctionOrderPort.refundOrderWithLock(AUCTION_ID)).willReturn(order);
        given(settlementRepository.findByAuctionIdForUpdate(AUCTION_ID)).willReturn(Optional.of(settlement));

        // when & then
        assertThatThrownBy(() -> paymentRefundUseCase.processRefund(AUCTION_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.SETTLEMENT_ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("실패: 주문을 찾을 수 없음")
    void processRefund_Fail_OrderNotFound() {
        // given
        given(auctionOrderPort.refundOrderWithLock(AUCTION_ID))
                .willThrow(new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> paymentRefundUseCase.processRefund(AUCTION_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.AUCTION_ORDER_NOT_FOUND);
    }

    private PaymentMember createMockMember(Long memberId) {
        return PaymentMember.builder()
                .publicId("uuid-" + memberId)
                .nickname("user" + memberId)
                .build();
    }
}
