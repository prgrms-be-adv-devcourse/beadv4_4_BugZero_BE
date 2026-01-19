package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionPaymentTimeoutEvent;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionPaymentTimeoutResponse;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionProcessTimeoutUseCaseTest {

    @InjectMocks
    private AuctionProcessTimeoutUseCase useCase;

    @Mock
    private AuctionOrderRepository orderRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AuctionOrder testOrder;
    private Auction testAuction;

    @BeforeEach
    void setUp() {
        // 테스트용 AuctionOrder 생성
        testOrder = AuctionOrder.builder()
                .auctionId(1L)
                .sellerId(100L)
                .bidderId(200L)
                .finalPrice(50000)
                .build();
        ReflectionTestUtils.setField(testOrder, "id", 1L);
        ReflectionTestUtils.setField(testOrder, "status", AuctionOrderStatus.PROCESSING);
        ReflectionTestUtils.setField(testOrder, "createdAt", LocalDateTime.now().minusHours(25));

        // 테스트용 Auction 생성
        testAuction = Auction.builder()
                .productId(10L)
                .sellerId(100L)
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().minusDays(1))
                .startPrice(10000)
                .tickSize(1000)
                .durationDays(7)
                .build();
        ReflectionTestUtils.setField(testAuction, "id", 1L);
        ReflectionTestUtils.setField(testAuction, "status", AuctionStatus.ENDED);
    }

    @Test
    @DisplayName("타임아웃 대상 주문이 없으면 빈 결과 반환")
    void execute_noTimeoutOrders_returnsEmptyResult() {
        // given
        given(orderRepository.findByStatusAndCreatedAtBefore(
                eq(AuctionOrderStatus.PROCESSING), any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        AuctionPaymentTimeoutResponse response = useCase.execute();

        // then
        assertThat(response.processedCount()).isZero();
        assertThat(response.details()).isEmpty();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("타임아웃 대상 주문 처리 성공")
    void execute_withTimeoutOrders_processesSuccessfully() {
        // given
        given(orderRepository.findByStatusAndCreatedAtBefore(
                eq(AuctionOrderStatus.PROCESSING), any(LocalDateTime.class)))
                .willReturn(List.of(testOrder));

        // when
        AuctionPaymentTimeoutResponse response = useCase.execute();

        // then
        assertThat(response.processedCount()).isEqualTo(1);
        assertThat(response.details()).hasSize(1);

        AuctionPaymentTimeoutResponse.TimeoutDetail detail = response.details().get(0);
        assertThat(detail.orderId()).isEqualTo(1L);
        assertThat(detail.auctionId()).isEqualTo(1L);
        assertThat(detail.buyerId()).isEqualTo(200L);
        assertThat(detail.status()).isEqualTo(AuctionOrderStatus.FAILED);

        // 주문 상태 변경 확인
        assertThat(testOrder.getStatus()).isEqualTo(AuctionOrderStatus.FAILED);

        // 경매 상태는 ENDED 유지 (변경 없음)
        assertThat(testAuction.getStatus()).isEqualTo(AuctionStatus.ENDED);

        // 이벤트 발행 확인
        verify(eventPublisher).publishEvent(any(AuctionPaymentTimeoutEvent.class));
    }

    @Test
    @DisplayName("여러 주문 중 일부 실패해도 나머지는 처리됨")
    void execute_partialFailure_continuesProcessing() {
        // given
        AuctionOrder order1 = AuctionOrder.builder()
                .auctionId(1L)
                .sellerId(100L)
                .bidderId(200L)
                .finalPrice(50000)
                .build();
        ReflectionTestUtils.setField(order1, "id", 1L);
        ReflectionTestUtils.setField(order1, "status", AuctionOrderStatus.PROCESSING);

        AuctionOrder order2 = AuctionOrder.builder()
                .auctionId(2L)
                .sellerId(101L)
                .bidderId(201L)
                .finalPrice(60000)
                .build();
        ReflectionTestUtils.setField(order2, "id", 2L);
        ReflectionTestUtils.setField(order2, "status", AuctionOrderStatus.PROCESSING);

        Auction auction2 = Auction.builder()
                .productId(11L)
                .sellerId(101L)
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().minusDays(1))
                .startPrice(20000)
                .tickSize(1000)
                .durationDays(7)
                .build();
        ReflectionTestUtils.setField(auction2, "id", 2L);
        ReflectionTestUtils.setField(auction2, "status", AuctionStatus.ENDED);

        given(orderRepository.findByStatusAndCreatedAtBefore(
                eq(AuctionOrderStatus.PROCESSING), any(LocalDateTime.class)))
                .willReturn(List.of(order1, order2));

        // when
        AuctionPaymentTimeoutResponse response = useCase.execute();

        // then
        assertThat(response.processedCount()).isEqualTo(2);
        assertThat(response.details()).hasSize(2);
    }

    @Test
    @DisplayName("응답에 requestTime이 포함됨")
    void execute_responseContainsRequestTime() {
        // given
        given(orderRepository.findByStatusAndCreatedAtBefore(
                eq(AuctionOrderStatus.PROCESSING), any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        LocalDateTime before = LocalDateTime.now();

        // when
        AuctionPaymentTimeoutResponse response = useCase.execute();

        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(response.requestTime()).isAfterOrEqualTo(before);
        assertThat(response.requestTime()).isBeforeOrEqualTo(after);
    }
}