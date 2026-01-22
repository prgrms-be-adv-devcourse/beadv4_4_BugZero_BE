package com.bugzero.rarego.boundedContext.payment.in;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.payment.app.PaymentAuctionTimeoutUseCase;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {
    private static final int BATCH_SIZE = 100;

    private final AuctionOrderPort auctionOrderPort;
    private final PaymentAuctionTimeoutUseCase paymentAuctionTimeoutUseCase;

    @Value("${auction.payment-timeout-days:3}")
    private int paymentTimeoutDays;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void checkPaymentTimeout() {
        log.info("결제 타임아웃 체크 시작");

        LocalDateTime deadline = LocalDateTime.now().minusDays(paymentTimeoutDays);

        int successCount = 0;
        int failCount = 0;
        int totalProcessed = 0;

        // 페이징 처리
        Slice<AuctionOrderDto> timeoutOrders;
        do {
            timeoutOrders = auctionOrderPort.findTimeoutOrders(deadline, PageRequest.of(0, BATCH_SIZE));

            for (AuctionOrderDto order : timeoutOrders) {
                try {
                    paymentAuctionTimeoutUseCase.processTimeout(order.auctionId());
                    successCount++;
                    log.info("타임아웃 처리 성공: auctionId={}", order.auctionId());
                } catch (Exception e) {
                    failCount++;
                    log.error("타임아웃 처리 실패: auctionId={}, error={}", order.auctionId(), e.getMessage());
                }
                totalProcessed++;
            }
        } while (timeoutOrders.hasNext());

        if (totalProcessed == 0) {
            log.info("타임아웃 대상 주문 없음");
        } else {
            log.info("결제 타임아웃 체크 완료: 총 {}건, 성공={}, 실패={}", totalProcessed, successCount, failCount);
        }
    }
}
