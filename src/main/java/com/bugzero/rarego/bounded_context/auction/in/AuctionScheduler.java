package com.bugzero.rarego.bounded_context.auction.in;

import com.bugzero.rarego.bounded_context.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;

/**
 * 경매별 종료 시간에 맞춰 동적으로 정산을 예약하는 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final TaskScheduler taskScheduler;
    private final AuctionSettleAuctionFacade facade;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 경매 종료 시간에 맞춰 정산 작업 예약
     */
    public void scheduleSettlement(Long auctionId, LocalDateTime endTime) {
        if (auctionId == null || endTime == null) {
            log.error("auctionId 또는 endTime이 null입니다. auctionId: {}, endTime: {}", auctionId, endTime);
            throw new CustomException(ErrorType.INVALID_INPUT);
        }

        try {
            cancelSchedule(auctionId);

            ZoneId zone = ZoneId.of("Asia/Seoul");
            Instant executionTime = endTime.atZone(zone).toInstant();
            Instant now = java.time.ZonedDateTime.now(zone).toInstant();

            if (executionTime.isBefore(now)) {
                log.warn("경매 {}의 종료 시간이 이미 지났습니다. 즉시 정산을 실행합니다.", auctionId);
                executeSettlement(auctionId);
                return;
            }

            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeSettlement(auctionId),
                    executionTime);

            scheduledTasks.put(auctionId, future);

            log.info("경매 {}의 정산이 {}에 예약되었습니다.", auctionId, endTime);

        } catch (RejectedExecutionException e) {
            log.error("스케줄러 용량 초과로 경매 {} 예약 실패", auctionId, e);
            throw new CustomException(ErrorType.SCHEDULER_CAPACITY_EXCEEDED);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("경매 {} 예약 중 예상치 못한 오류 발생", auctionId, e);
            throw new CustomException(ErrorType.AUCTION_SCHEDULE_FAILED);
        }
    }

    private void executeSettlement(Long auctionId) {
        try {
            log.info("경매 {} 정산 시작", auctionId);
            facade.settleOne(auctionId);
            log.info("경매 {} 정산 완료", auctionId);

        } catch (Exception e) {
            log.error("경매 {} 정산 실패", auctionId, e);

        } finally {
            scheduledTasks.remove(auctionId);
        }
    }

    public void cancelSchedule(Long auctionId) {
        if (auctionId == null) {
            return;
        }

        ScheduledFuture<?> future = scheduledTasks.remove(auctionId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.info("경매 {}의 정산 예약을 취소했습니다.", auctionId);
        }
    }

    public int getScheduledTaskCount() {
        scheduledTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
        return scheduledTasks.size();
    }

    public boolean isScheduled(Long auctionId) {
        if (auctionId == null) {
            return false;
        }

        ScheduledFuture<?> future = scheduledTasks.get(auctionId);
        return future != null && !future.isDone();
    }
}