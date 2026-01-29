package com.bugzero.rarego.boundedContext.auction.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionDetermineStartAuctionUseCase {

	private final AuctionRepository auctionRepository;

	@Transactional
	public Long determineStartAuction(Long productId) {

		//현재 경매일정이 정해지지 않은 경매만 가져올 수 있도록 함.
		Auction auction = auctionRepository.findByProductIdAndStartTimeIsNull(productId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

		auction.determineStart(determineStartTime());

		return auction.getId();
	}

	//시작 시간 설정. 24시간뒤 정각으로
	private LocalDateTime determineStartTime() {
		LocalDateTime startTime = LocalDateTime.now();
		// 분, 초, 나노초가 0이면 그대로 반환
		if (startTime.getMinute() == 0 &&
			startTime.getSecond() == 0 &&
			startTime.getNano() == 0) {
			return startTime;
		}

		// 아니면 다음 정각으로 올림
		return startTime
			.plusHours(1)
			.withMinute(0)
			.withSecond(0)
			.withNano(0)
			.plusHours(24);
	}

}
