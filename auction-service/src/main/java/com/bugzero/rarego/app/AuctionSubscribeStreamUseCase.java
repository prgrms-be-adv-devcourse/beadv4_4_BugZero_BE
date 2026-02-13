package com.bugzero.rarego.app;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionSubscribeStreamUseCase {

	private static final int MAX_SUBSCRIBERS_PER_AUCTION = 1000;

	private final AuctionBidStreamSupport streamSupport;
	private final AuctionRepository auctionRepository;

	public SseEmitter execute(Long auctionId) {
		Auction auction = findAuction(auctionId);
		validateAuctionInProgress(auction);
		validateSubscriberLimit(auctionId);

		return streamSupport.subscribe(auctionId, auction.getCurrentPriceOrStartPrice());
	}

	public int getTotalSubscribers() {
		return streamSupport.getTotalSubscribers();
	}

	public int getAuctionSubscribers(Long auctionId) {
		return streamSupport.getAuctionSubscribers(auctionId);
	}

	// ------- Helper Method -------

	private Auction findAuction(Long auctionId) {
		return auctionRepository.findById(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));
	}

	// TODO: 성능을 위해 엄격한 동기화 대신 soft limit 적용
	// 동시 요청 시 소폭 초과 가능 - 멘토링 후 확인 예정
	private void validateSubscriberLimit(Long auctionId) {
		int current = streamSupport.getAuctionSubscribers(auctionId);
		if (current >= MAX_SUBSCRIBERS_PER_AUCTION) {
			log.warn("경매 {} 구독자 수 한도 초과 - 현재: {}", auctionId, current);
			throw new CustomException(ErrorType.SERVICE_SUBSCRIBER_LIMIT_EXCEEDED);
		}
	}

	private void validateAuctionInProgress(Auction auction) {
		if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
			throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS);
		}
	}
}