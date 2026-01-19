package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionPaymentTimeoutEvent;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionPaymentTimeoutResponse;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionPaymentTimeoutResponse.TimeoutDetail;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionProcessTimeoutUseCase {

    private final AuctionOrderRepository orderRepository;
    private final AuctionRepository auctionRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int TIMEOUT_HOURS = 24;

    @Transactional
    public AuctionPaymentTimeoutResponse execute() {
        LocalDateTime requestTime = LocalDateTime.now();
        LocalDateTime cutoffTime = requestTime.minusHours(TIMEOUT_HOURS);

        List<AuctionOrder> timedOutOrders = orderRepository
                .findByStatusAndCreatedAtBefore(AuctionOrderStatus.PROCESSING, cutoffTime);

        log.info("타임아웃 대상 주문 수: {}", timedOutOrders.size());

        List<TimeoutDetail> details = new ArrayList<>();

        for (AuctionOrder order : timedOutOrders) {
            try {
                TimeoutDetail detail = processTimeoutOrder(order);
                details.add(detail);
            } catch (Exception e) {
                log.error("주문 {} 타임아웃 처리 실패 - 이유: {}", order.getId(), e.getMessage());
            }
        }

        return new AuctionPaymentTimeoutResponse(
                requestTime,
                details.size(),
                details
        );
    }

    private TimeoutDetail processTimeoutOrder(AuctionOrder order) {
        order.timeout();

        int penaltyAmount = mockForfeitDeposit(order.getBidderId(), order.getAuctionId());

        eventPublisher.publishEvent(new AuctionPaymentTimeoutEvent(
                order.getAuctionId(),
                order.getBidderId(),
                order.getSellerId(),
                penaltyAmount
        ));

        log.info("타임아웃 처리 완료 - orderId: {}, auctionId: {}, bidderId: {}, penalty: {}",
                order.getId(), order.getAuctionId(), order.getBidderId(), penaltyAmount);

        return new TimeoutDetail(
                order.getId(),
                order.getAuctionId(),
                order.getBidderId(),
                penaltyAmount,
                AuctionOrderStatus.FAILED
        );
    }

    private int mockForfeitDeposit(Long buyerId, Long auctionId) {
        log.warn("[MOCK] 보증금 몰수 처리 - buyerId: {}, auctionId: {} (실제 구현 필요)", buyerId, auctionId);
        return 0;
    }
}