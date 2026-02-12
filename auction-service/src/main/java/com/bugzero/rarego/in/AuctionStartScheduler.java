package com.bugzero.rarego.in;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionStartScheduler {

	private final AuctionRepository auctionRepository;

	// 1분마다 실행
	@Scheduled(cron = "0 * * * * *")
	@Transactional
	public void autoStartAuctions() {
		LocalDateTime now = LocalDateTime.now();

		// 시작 시간이 되었는데 아직 시작 안 한 경매 찾기
		List<Auction> pendingAuctions = auctionRepository.findAllByStatusAndStartTimeBefore(
			AuctionStatus.SCHEDULED, now
		);

		if (pendingAuctions.isEmpty()) {
			return;
		}

		log.info("경매 자동 시작 스케줄러 실행: {}건 시작 처리", pendingAuctions.size());

		for (Auction auction : pendingAuctions) {
			try {
				auction.start();
			} catch (Exception e) {
				log.error("경매 ID {} 시작 처리 중 오류 발생", auction.getId(), e);
			}
		}
	}
}
