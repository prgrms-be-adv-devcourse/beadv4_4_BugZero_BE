package com.bugzero.rarego.boundedContext.payment.app;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;

@ExtendWith(MockitoExtension.class)
class PaymentTimeoutSchedulerTest {

    @InjectMocks
    private PaymentTimeoutScheduler paymentTimeoutScheduler;

    @Mock
    private AuctionOrderPort auctionOrderPort;

    @Mock
    private PaymentAuctionTimeoutUseCase paymentAuctionTimeoutUseCase;

    @Test
    @DisplayName("성공: 타임아웃 대상 주문이 있으면 processTimeout을 호출한다")
    void checkPaymentTimeout_Success_ProcessesTimeoutOrders() {
        // given
        ReflectionTestUtils.setField(paymentTimeoutScheduler, "paymentTimeoutDays", 3);

        AuctionOrderDto order1 = new AuctionOrderDto(1L, 100L, 10L, 20L, 50000, "PROCESSING",
                LocalDateTime.now().minusDays(5));
        AuctionOrderDto order2 = new AuctionOrderDto(2L, 200L, 11L, 21L, 60000, "PROCESSING",
                LocalDateTime.now().minusDays(4));

        given(auctionOrderPort.findTimeoutOrders(any(LocalDateTime.class)))
                .willReturn(List.of(order1, order2));

        // when
        paymentTimeoutScheduler.checkPaymentTimeout();

        // then
        then(paymentAuctionTimeoutUseCase).should(times(1)).processTimeout(100L);
        then(paymentAuctionTimeoutUseCase).should(times(1)).processTimeout(200L);
    }

    @Test
    @DisplayName("성공: 타임아웃 대상 주문이 없으면 processTimeout을 호출하지 않는다")
    void checkPaymentTimeout_Success_NoTimeoutOrders() {
        // given
        ReflectionTestUtils.setField(paymentTimeoutScheduler, "paymentTimeoutDays", 3);

        given(auctionOrderPort.findTimeoutOrders(any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        paymentTimeoutScheduler.checkPaymentTimeout();

        // then
        then(paymentAuctionTimeoutUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성공: 일부 주문 처리 실패해도 나머지는 계속 처리한다")
    void checkPaymentTimeout_PartialFailure_ContinuesProcessing() {
        // given
        ReflectionTestUtils.setField(paymentTimeoutScheduler, "paymentTimeoutDays", 3);

        AuctionOrderDto order1 = new AuctionOrderDto(1L, 100L, 10L, 20L, 50000, "PROCESSING",
                LocalDateTime.now().minusDays(5));
        AuctionOrderDto order2 = new AuctionOrderDto(2L, 200L, 11L, 21L, 60000, "PROCESSING",
                LocalDateTime.now().minusDays(4));
        AuctionOrderDto order3 = new AuctionOrderDto(3L, 300L, 12L, 22L, 70000, "PROCESSING",
                LocalDateTime.now().minusDays(4));

        given(auctionOrderPort.findTimeoutOrders(any(LocalDateTime.class)))
                .willReturn(List.of(order1, order2, order3));

        // order2 처리 시 예외 발생
        doNothing().when(paymentAuctionTimeoutUseCase).processTimeout(100L);
        doThrow(new RuntimeException("처리 실패")).when(paymentAuctionTimeoutUseCase).processTimeout(200L);
        doNothing().when(paymentAuctionTimeoutUseCase).processTimeout(300L);

        // when
        paymentTimeoutScheduler.checkPaymentTimeout();

        // then - order2 실패해도 order3는 처리됨
        then(paymentAuctionTimeoutUseCase).should(times(1)).processTimeout(100L);
        then(paymentAuctionTimeoutUseCase).should(times(1)).processTimeout(200L);
        then(paymentAuctionTimeoutUseCase).should(times(1)).processTimeout(300L);
    }
}
