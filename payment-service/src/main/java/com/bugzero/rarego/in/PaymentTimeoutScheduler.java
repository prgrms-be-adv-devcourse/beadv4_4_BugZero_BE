package com.bugzero.rarego.in;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.PaymentAuctionTimeoutUseCase;
import com.bugzero.rarego.out.AuctionOrderApiClient;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {
	private static final int BATCH_SIZE = 100;

	private final AuctionOrderApiClient auctionOrderApiClient;
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

		// 처리 중 상태가 변경되므로 항상 0페이지를 조회
		AuctionOrderApiClient.AuctionOrderSlice timeoutOrders;
		while (true) {
			timeoutOrders = auctionOrderApiClient.findTimeoutOrders(deadline, PageRequest.of(0, BATCH_SIZE));
			if (timeoutOrders.content().isEmpty()) {
				break;
			}

			int batchSuccess = 0;
			for (AuctionOrderDto order : timeoutOrders.content()) {
				try {
					paymentAuctionTimeoutUseCase.processTimeout(order.auctionId());
					successCount++;
					batchSuccess++;
					log.info("타임아웃 처리 성공: auctionId={}", order.auctionId());
				} catch (Exception e) {
					failCount++;
					log.error("타임아웃 처리 실패: auctionId={}, error={}", order.auctionId(), e.getMessage());
				}
				totalProcessed++;
			}

			if (batchSuccess == 0) {
				log.warn("타임아웃 처리 진행 없음. 반복을 중단합니다.");
				break;
			}
		}

		if (totalProcessed == 0) {
			log.info("타임아웃 대상 주문 없음");
		} else {
			log.info("결제 타임아웃 체크 완료: 총 {}건, 성공={}, 실패={}", totalProcessed, successCount, failCount);
		}
	}
}
