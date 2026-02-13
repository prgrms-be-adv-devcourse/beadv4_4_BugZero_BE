package com.bugzero.rarego.in;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionSchedulerInitializer {

	private final AuctionRepository auctionRepository;
	private final AuctionScheduler scheduler;

	@EventListener(ApplicationReadyEvent.class)
	public void initializeSchedules() {
		log.info("경매 정산 스케줄 복구 시작...");

		try {
			LocalDateTime now = LocalDateTime.now();

			// 락 없이 단순 조회 (ID만 필요)
			Page<Auction> auctionPage = auctionRepository.findAllByStatusAndEndTimeIsNotNull(
				AuctionStatus.IN_PROGRESS,
				PageRequest.of(0, 1000)
			);

			List<Auction> inProgressAuctions = auctionPage.getContent();

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
						// 즉시 실행이 아닌 스케줄만 등록
						scheduler.scheduleSettlement(auction.getId(), now.plusSeconds(5));
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
