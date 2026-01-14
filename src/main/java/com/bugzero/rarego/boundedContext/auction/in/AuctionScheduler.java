package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionSettlementFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionSettlementFacade facade;

    @Scheduled(cron = "0 * * * * *")
    public void settleExpiredAuctions() {
        try {
            facade.settle();
        } catch (Exception e) {
            log.error("경매 자동 처리 실패", e);
        }
    }
}
