package com.bugzero.rarego.in;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.PaymentFacade;
import com.bugzero.rarego.out.AuctionOrderApiClient;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiringSoonScheduler {
	private static final int BATCH_SIZE = 100;

	private final AuctionOrderApiClient auctionOrderApiClient;
	private final PaymentFacade paymentFacade;

	@Value("${auction.payment-timeout-days:3}")
	private int paymentTimeoutDays;

	/**
	 * 매일 오전 10시에 실행
	 * - 사용자가 깨어있는 시간에 알림을 보내 반응률을 높임
	 * - 자정에 도는 타임아웃 배치와 DB 부하를 분산시킴
	 */
	@Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
	public void checkExpiringSoon() {
		log.info("낙찰 결제 마감 임박 체크 시작");

		LocalDateTime targetEndedAt = LocalDate.now().minusDays(paymentTimeoutDays).atTime(LocalTime.MAX);

		int successCount = 0;
		int failCount = 0;
		int totalProcessed = 0;

		while (true) {
			AuctionOrderApiClient.AuctionOrderSlice orders = auctionOrderApiClient.findExpiringSoonOrders(
				targetEndedAt,
				PageRequest.of(0, BATCH_SIZE)
			);

			if (orders.content().isEmpty()) {
				break;
			}

			for (AuctionOrderDto order : orders.content()) {
				try {
					LocalDateTime expiredAt = LocalDate.now().atTime(LocalTime.MAX);

					// 결제 마감 임박 이벤트 발행
					paymentFacade.publishExpiringSoonEvent(order, expiredAt);

					// 이벤트 발행 완료 처리 (중복 발행 방지)
					auctionOrderApiClient.markAsNoticed(order.orderId());

					successCount++;
					log.info("마감 임박 처리 완료: auctionId={}", order.auctionId());

				} catch (Exception e) {
					failCount++;
					log.error("마감 임박 알림 실패: auctionId={}", order.auctionId(), e);
				}
				totalProcessed++;
			}
		}

		if (totalProcessed == 0) {
			log.info("마감 임박 알림 대상 없음");
		} else {
			log.info("결제 마감 임박 알림 완료: 총 {}건, 성공={}, 실패={}", totalProcessed, successCount, failCount);
		}
	}
}
