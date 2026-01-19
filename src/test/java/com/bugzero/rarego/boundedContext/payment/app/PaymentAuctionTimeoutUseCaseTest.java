package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.DepositRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;
import com.bugzero.rarego.shared.payment.event.PaymentTimeoutEvent;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentAuctionTimeoutUseCaseTest {

    @InjectMocks
    private PaymentAuctionTimeoutUseCase paymentAuctionTimeoutUseCase;

    @Mock
    private AuctionOrderPort auctionOrderPort;

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private PaymentSupport paymentSupport;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Long AUCTION_ID = 100L;
    private static final Long BIDDER_ID = 1L;
    private static final Long SELLER_ID = 2L;
    private static final int FINAL_PRICE = 100000;
    private static final int DEPOSIT_AMOUNT = 10000;

    @Test
    @DisplayName("성공: 타임아웃 처리 후 PaymentTimeoutEvent 발행")
    void processTimeout_Success_PublishesEvent() {
        // given
        AuctionOrderDto order = new AuctionOrderDto(
                1L, AUCTION_ID, SELLER_ID, BIDDER_ID, FINAL_PRICE, "PROCESSING", LocalDateTime.now().minusDays(4));

        PaymentMember buyer = createMockMember(BIDDER_ID);
        PaymentMember seller = createMockMember(SELLER_ID);
        Deposit deposit = createMockDeposit(buyer, AUCTION_ID, DEPOSIT_AMOUNT);
        Wallet wallet = Wallet.builder().balance(50000).holdingAmount(DEPOSIT_AMOUNT).build();

        given(auctionOrderPort.findByAuctionIdForUpdate(AUCTION_ID)).willReturn(Optional.of(order));
        given(depositRepository.findByMemberIdAndAuctionId(BIDDER_ID, AUCTION_ID)).willReturn(Optional.of(deposit));
        given(paymentSupport.findWalletByMemberIdForUpdate(BIDDER_ID)).willReturn(wallet);
        given(paymentSupport.findMemberById(BIDDER_ID)).willReturn(buyer);
        given(paymentSupport.findMemberById(SELLER_ID)).willReturn(seller);

        // when
        paymentAuctionTimeoutUseCase.processTimeout(AUCTION_ID);

        // then
        ArgumentCaptor<PaymentTimeoutEvent> eventCaptor = ArgumentCaptor.forClass(PaymentTimeoutEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PaymentTimeoutEvent event = eventCaptor.getValue();
        assertThat(event.auctionId()).isEqualTo(AUCTION_ID);
        assertThat(event.buyerId()).isEqualTo(BIDDER_ID);
        assertThat(event.sellerId()).isEqualTo(SELLER_ID);
        assertThat(event.penaltyAmount()).isEqualTo(DEPOSIT_AMOUNT);
    }

    @Test
    @DisplayName("성공: 보증금 몰수 및 주문 실패 처리")
    void processTimeout_Success_ForfeitsDepositAndFailsOrder() {
        // given
        AuctionOrderDto order = new AuctionOrderDto(
                1L, AUCTION_ID, SELLER_ID, BIDDER_ID, FINAL_PRICE, "PROCESSING", LocalDateTime.now().minusDays(4));

        PaymentMember buyer = createMockMember(BIDDER_ID);
        PaymentMember seller = createMockMember(SELLER_ID);
        Deposit deposit = createMockDeposit(buyer, AUCTION_ID, DEPOSIT_AMOUNT);
        Wallet wallet = Wallet.builder().balance(50000).holdingAmount(DEPOSIT_AMOUNT).build();

        given(auctionOrderPort.findByAuctionIdForUpdate(AUCTION_ID)).willReturn(Optional.of(order));
        given(depositRepository.findByMemberIdAndAuctionId(BIDDER_ID, AUCTION_ID)).willReturn(Optional.of(deposit));
        given(paymentSupport.findWalletByMemberIdForUpdate(BIDDER_ID)).willReturn(wallet);
        given(paymentSupport.findMemberById(BIDDER_ID)).willReturn(buyer);
        given(paymentSupport.findMemberById(SELLER_ID)).willReturn(seller);

        // when
        paymentAuctionTimeoutUseCase.processTimeout(AUCTION_ID);

        // then
        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.FORFEITED);
        assertThat(wallet.getHoldingAmount()).isEqualTo(0);
        verify(auctionOrderPort).failOrder(AUCTION_ID);
        verify(settlementRepository).save(any());
    }

    @Test
    @DisplayName("실패: 주문을 찾을 수 없음")
    void processTimeout_Fail_OrderNotFound() {
        // given
        given(auctionOrderPort.findByAuctionIdForUpdate(AUCTION_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentAuctionTimeoutUseCase.processTimeout(AUCTION_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.AUCTION_ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 주문 상태가 PROCESSING이 아님")
    void processTimeout_Fail_InvalidOrderStatus() {
        // given
        AuctionOrderDto order = new AuctionOrderDto(
                1L, AUCTION_ID, SELLER_ID, BIDDER_ID, FINAL_PRICE, "SUCCESS", LocalDateTime.now().minusDays(4));

        given(auctionOrderPort.findByAuctionIdForUpdate(AUCTION_ID)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> paymentAuctionTimeoutUseCase.processTimeout(AUCTION_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_ORDER_STATUS);
    }

    private PaymentMember createMockMember(Long memberId) {
        return PaymentMember.builder()
                .publicId("uuid-member-" + memberId)
                .nickname("테스트유저" + memberId)
                .build();
    }

    private Deposit createMockDeposit(PaymentMember member, Long auctionId, int amount) {
        return Deposit.create(member, auctionId, amount);
    }
}
