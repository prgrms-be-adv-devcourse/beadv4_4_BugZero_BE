package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.DepositRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;

@ExtendWith(MockitoExtension.class)
class PaymentAuctionFinalUseCaseTest {

        @InjectMocks
        private PaymentAuctionFinalUseCase paymentAuctionFinalUseCase;

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

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(paymentAuctionFinalUseCase, "paymentTimeoutDays", 3);
        }

        @Test
        @DisplayName("성공: 낙찰 결제 완료")
        void finalPayment_Success() {
                // given
                Long memberId = 1L;
                Long sellerId = 5L;
                Long auctionId = 100L;
                int finalPrice = 100000;
                int depositAmount = 10000;
                int expectedPaymentAmount = finalPrice - depositAmount;

                AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
                                "홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

                AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, sellerId, memberId, finalPrice, "PROCESSING",
                                LocalDateTime.now());

                PaymentMember buyer = mock(PaymentMember.class);
                when(buyer.getPublicId()).thenReturn("uuid-member-1");

                PaymentMember seller = mock(PaymentMember.class);

                Deposit deposit = Deposit.create(buyer, auctionId, depositAmount);
                Wallet wallet = Wallet.builder().balance(200000).holdingAmount(depositAmount).build();

                when(auctionOrderPort.findByAuctionId(auctionId)).thenReturn(Optional.of(order));
                when(depositRepository.findByMemberIdAndAuctionId(memberId, auctionId))
                                .thenReturn(Optional.of(deposit));
                when(paymentSupport.findWalletByMemberIdForUpdate(memberId)).thenReturn(wallet);
                when(paymentSupport.findMemberById(memberId)).thenReturn(buyer);
                when(paymentSupport.findMemberById(sellerId)).thenReturn(seller);

                // when
                AuctionFinalPaymentResponseDto response = paymentAuctionFinalUseCase.finalPayment(memberId, auctionId,
                                request);

                // then
                assertThat(response.auctionId()).isEqualTo(auctionId);
                assertThat(response.finalPrice()).isEqualTo(finalPrice);
                assertThat(response.depositAmount()).isEqualTo(depositAmount);
                assertThat(response.paidAmount()).isEqualTo(expectedPaymentAmount);
                assertThat(response.status()).isEqualTo("PAID");

                // Wallet 잔액 검증
                int expectedBalance = 200000 - depositAmount - expectedPaymentAmount;
                assertThat(wallet.getBalance()).isEqualTo(expectedBalance);
                assertThat(wallet.getHoldingAmount()).isEqualTo(0);

                // Deposit 상태 검증
                assertThat(deposit.getStatus()).isEqualTo(DepositStatus.USED);

                // 트랜잭션 이력 2건 (보증금 사용, 잔금 결제)
                verify(transactionRepository, times(2)).save(any(PaymentTransaction.class));
                verify(auctionOrderPort).completeOrder(auctionId);
        }

        @Test
        @DisplayName("실패: 주문 정보 없음")
        void finalPayment_OrderNotFound() {
                // given
                Long memberId = 1L;
                Long auctionId = 100L;
                AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
                                "홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

                when(auctionOrderPort.findByAuctionId(auctionId)).thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberId, auctionId, request))
                                .isInstanceOf(CustomException.class)
                                .extracting("errorType")
                                .isEqualTo(ErrorType.AUCTION_ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 낙찰자 아님")
        void finalPayment_NotWinner() {
                // given
                Long memberId = 1L;
                Long auctionId = 100L;
                Long winnerId = 999L; // 다른 사람

                AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
                                "홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");
                AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, 5L, winnerId, 100000, "PROCESSING",
                                LocalDateTime.now());

                when(auctionOrderPort.findByAuctionId(auctionId)).thenReturn(Optional.of(order));

                // when & then
                assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberId, auctionId, request))
                                .isInstanceOf(CustomException.class)
                                .extracting("errorType")
                                .isEqualTo(ErrorType.NOT_AUCTION_WINNER);
        }

        @Test
        @DisplayName("실패: 주문 상태가 PROCESSING 아님")
        void finalPayment_InvalidOrderStatus() {
                // given
                Long memberId = 1L;
                Long auctionId = 100L;

                AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
                                "홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");
                AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, 5L, memberId, 100000, "SUCCESS",
                                LocalDateTime.now()); // 이미 완료

                when(auctionOrderPort.findByAuctionId(auctionId)).thenReturn(Optional.of(order));

                // when & then
                assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberId, auctionId, request))
                                .isInstanceOf(CustomException.class)
                                .extracting("errorType")
                                .isEqualTo(ErrorType.INVALID_ORDER_STATUS);
        }

        @Test
        @DisplayName("실패: 보증금 없음")
        void finalPayment_DepositNotFound() {
                // given
                Long memberId = 1L;
                Long auctionId = 100L;

                AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
                                "홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");
                AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, 5L, memberId, 100000, "PROCESSING",
                                LocalDateTime.now());

                when(auctionOrderPort.findByAuctionId(auctionId)).thenReturn(Optional.of(order));
                when(depositRepository.findByMemberIdAndAuctionId(memberId, auctionId)).thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberId, auctionId, request))
                                .isInstanceOf(CustomException.class)
                                .extracting("errorType")
                                .isEqualTo(ErrorType.DEPOSIT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 잔액 부족")
        void finalPayment_InsufficientBalance() {
                // given
                Long memberId = 1L;
                Long auctionId = 100L;
                int finalPrice = 100000;
                int depositAmount = 10000;

                AuctionFinalPaymentRequestDto request = new AuctionFinalPaymentRequestDto(
                                "홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");
                AuctionOrderDto order = new AuctionOrderDto(1L, auctionId, 5L, memberId, finalPrice, "PROCESSING",
                                LocalDateTime.now());

                PaymentMember member = mock(PaymentMember.class);

                Deposit deposit = Deposit.create(member, auctionId, depositAmount);
                Wallet wallet = Wallet.builder().balance(50000).holdingAmount(depositAmount).build(); // 잔액 부족

                when(auctionOrderPort.findByAuctionId(auctionId)).thenReturn(Optional.of(order));
                when(depositRepository.findByMemberIdAndAuctionId(memberId, auctionId))
                                .thenReturn(Optional.of(deposit));
                when(paymentSupport.findWalletByMemberIdForUpdate(memberId)).thenReturn(wallet);
                when(paymentSupport.findMemberById(memberId)).thenReturn(member);

                // when & then
                assertThatThrownBy(() -> paymentAuctionFinalUseCase.finalPayment(memberId, auctionId, request))
                                .isInstanceOf(CustomException.class)
                                .extracting("errorType")
                                .isEqualTo(ErrorType.INSUFFICIENT_BALANCE);
        }
}
