package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 서버 시작 시 진행 중인 경매들의 정산 예약을 복구
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionSchedulerInitializer {

    private final AuctionRepository auctionRepository;
    private final AuctionScheduler scheduler;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchedules() {
        log.info("경매 정산 스케줄 복구 시작...");

        try {
            LocalDateTime now = LocalDateTime.now();

            // 진행 중인 모든 경매 조회
            List<Auction> inProgressAuctions = auctionRepository.findExpiredInProgressAuctionsWithLock(
                    LocalDateTime.now().plusYears(100), // 미래 시간으로 조회하여 모든 진행중 경매 가져오기
                    PageRequest.of(0, 1000)
            );

            if (inProgressAuctions.isEmpty()) {
                log.info("복구할 진행 중인 경매가 없습니다.");
                return;
            }

            int scheduled = 0;
            int expired = 0;

            for (Auction auction : inProgressAuctions) {
                try {
                    if (auction.getEndTime().isAfter(now)) {
                        scheduler.scheduleSettlement(auction.getId(), auction.getEndTime());
                        scheduled++;
                    } else {
                        scheduler.scheduleSettlement(auction.getId(), auction.getEndTime());
                        expired++;
                    }
                } catch (Exception e) {
                    log.error("경매 {} 예약 복구 실패", auction.getId(), e);
                }
            }

            log.info("경매 정산 스케줄 복구 완료 - 예약: {}건, 즉시 실행: {}건", scheduled, expired);

        } catch (Exception e) {
            log.error("경매 정산 스케줄 복구 중 오류 발생", e);
        }
    }
}