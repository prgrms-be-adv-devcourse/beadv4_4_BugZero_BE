package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {
    private final AuctionOrderPort auctionOrderPort;
    private final PaymentAuctionTimeoutUseCase paymentAuctionTimeoutUseCase;

    @Value("${auction.payment-timeout-days:3}")
    private int paymentTimeoutDays;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    public void checkPaymentTimeout() {
        log.info("결제 타임아웃 체크 시작");

        LocalDateTime deadline = LocalDateTime.now().minusDays(paymentTimeoutDays);
        List<AuctionOrderDto> timeoutOrders = auctionOrderPort.findTimeoutOrders(deadline);

        if (timeoutOrders.isEmpty()) {
            log.info("타임아웃 대상 주문 없음");
            return;
        }

        log.info("타임아웃 대상 주문 {}건 발견", timeoutOrders.size());

        int successCount = 0;
        int failCount = 0;

        for (AuctionOrderDto order : timeoutOrders) {
            try {
                paymentAuctionTimeoutUseCase.processTimeout(order.auctionId());
                successCount++;
                log.info("타임아웃 처리 성공: auctionId={}", order.auctionId());
            } catch (Exception e) {
                failCount++;
                log.error("타임아웃 처리 실패: auctionId={}, error={}", order.auctionId(), e.getMessage());
            }
        }

        log.info("결제 타임아웃 체크 완료: 성공={}, 실패={}", successCount, failCount);
    }
}
