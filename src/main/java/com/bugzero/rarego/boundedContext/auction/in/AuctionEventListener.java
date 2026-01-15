package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 경매 생성/수정 시 자동으로 정산 작업을 예약
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {

    private final AuctionScheduler scheduler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionCreated(AuctionCreatedEvent event) {
        try {
            if (event == null || event.getAuctionId() == null || event.getEndTime() == null) {
                log.error("유효하지 않은 AuctionCreatedEvent: {}", event);
                return;
            }

            log.info("경매 생성 이벤트 수신 - auctionId: {}", event.getAuctionId());
            scheduler.scheduleSettlement(event.getAuctionId(), event.getEndTime());

        } catch (Exception e) {
            log.error("경매 {} 생성 이벤트 처리 실패", event.getAuctionId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionUpdated(AuctionUpdatedEvent event) {
        try {
            if (event == null || event.getAuctionId() == null || event.getNewEndTime() == null) {
                log.error("유효하지 않은 AuctionUpdatedEvent: {}", event);
                return;
            }

            log.info("경매 수정 이벤트 수신 - auctionId: {}", event.getAuctionId());
            scheduler.cancelSchedule(event.getAuctionId());
            scheduler.scheduleSettlement(event.getAuctionId(), event.getNewEndTime());

        } catch (Exception e) {
            log.error("경매 {} 수정 이벤트 처리 실패", event.getAuctionId(), e);
        }
    }
}