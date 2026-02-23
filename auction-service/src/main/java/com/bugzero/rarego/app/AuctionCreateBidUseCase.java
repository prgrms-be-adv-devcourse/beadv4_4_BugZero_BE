package com.bugzero.rarego.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.config.AuctionMetrics;
import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.domain.AuctionMember;
import com.bugzero.rarego.domain.Bid;
import com.bugzero.rarego.domain.event.AuctionBidCreatedEvent;
import com.bugzero.rarego.domain.event.AuctionUpdatedEvent;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.lock.DistributedLock;
import com.bugzero.rarego.global.outbox.app.OutboxUseCase;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.in.dto.BidResponseDto;
import com.bugzero.rarego.out.BidRepository;
import com.bugzero.rarego.out.es.ProductSearchClient;
import com.bugzero.rarego.shared.auction.event.AuctionOutbidEvent;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionCreateBidUseCase {

	private final AuctionSupport support;
	private final BidRepository bidRepository;
	private final ProductSearchClient productSearchClient;
	private final OutboxUseCase outboxUseCase;
	private final ApplicationEventPublisher eventPublisher;
	private final AuctionMetrics auctionMetrics;

	@DistributedLock(key = "'auction:bid:' + #auctionId")
	public BidResponseDto createBid(Long auctionId, String memberPublicId, int bidAmount) {
		// 입찰 시도 메트릭 기록
		auctionMetrics.incrementBidTotal();

		// 회원 조회
		AuctionMember bidder = support.getPublicMember(memberPublicId);

		// 경매 조회
		Auction auction = support.findAuctionById(auctionId);

		// 유효성 검증
		Optional<Bid> lastBid = validateBid(auction, bidder, bidAmount);

		// 마감 임박 연장 로직
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime originalEndTime = auction.getEndTime(); // 연장 전 종료 시간 저장

		boolean isExtended = auction.extendEndTimeIfClose(now);

		// 입찰 정보 저장 (bidder.getId() 사용)
		Bid bid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(bidder.getId())
			.bidAmount(bidAmount)
			.bidTime(now)
			.build();

		// 현재가 갱신
		bidRepository.save(bid);
		auction.updateCurrentPrice(bidAmount);

		// 입찰 생성 이벤트 발행
		eventPublisher.publishEvent(
			AuctionBidCreatedEvent.of(auctionId, bidder.getId(), bidAmount)
		);

		// 추월당한 기존 최고 입찰자에게 아웃박스 이벤트 저장
		lastBid.ifPresent(prevBid -> {
			AuctionOutbidEvent outbidEvent = new AuctionOutbidEvent(
				auctionId,
				getProductName(auction.getProductId()),
				bidder.getId(),
				bidAmount,
				prevBid.getBidderId()
			);

			outboxUseCase.saveOutbox(outbidEvent);
		});

		if (isExtended) {
			eventPublisher.publishEvent(new AuctionUpdatedEvent(
				auction.getId(),
				originalEndTime,
				auction.getEndTime()
			));
		}

		// 입찰 성공 메트릭 기록
		auctionMetrics.incrementBidSuccess();

		return BidResponseDto.from(
			bid,
			bidder.getPublicId(),
			Long.valueOf(auction.getCurrentPrice())
		);
	}

	private Optional<Bid> validateBid(Auction auction, AuctionMember bidder, int bidAmount) {
		// 경매가 진행중이 아닐 때 입찰 방지
		if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
			auctionMetrics.incrementBidFailNotInProgress();
			throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS, "경매가 진행 중인 상태가 아닙니다.");
		}

		// 판매자 본인 입찰 방지 (ID 비교)
		if (auction.getSellerId().equals(bidder.getId())) {
			auctionMetrics.incrementBidFailSellerBid();
			throw new CustomException(ErrorType.AUCTION_SELLER_CANNOT_BID, "본인 경매에는 입찰할 수 없습니다.");
		}

		// 연속 입찰 방지 (현재 최고 입찰자 = 본인이면 거절)
		Optional<Bid> lastBid = bidRepository.findTopByAuctionIdOrderByBidTimeDesc(auction.getId());
		if (lastBid.isPresent() && lastBid.get().getBidderId().equals(bidder.getId())) {
			auctionMetrics.incrementBidFailAlreadyHighest();
			throw new CustomException(ErrorType.AUCTION_ALREADY_HIGHEST_BIDDER, "연속 입찰은 불가합니다.");
		}

		// 경매 입찰 가능한 시간인지에 대한 검증
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) {
			auctionMetrics.incrementBidFailNotInProgress();
			throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS, "경매 시간이 아닙니다.");
		}

		// 입찰 금액 검증
		int minimumBid = lastBid.isEmpty() ? auction.getStartPrice()
			: auction.getCurrentPrice() + auction.getTickSize();

		if (bidAmount < minimumBid) {
			auctionMetrics.incrementBidFailAmountTooLow();
			throw new CustomException(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW, "입찰 금액이 유효하지 않습니다.");
		}

		return lastBid;
	}

	private String getProductName(Long productId) {
		try {
			return productSearchClient.getProduct(productId)
				.map(ProductAuctionResponseDto::name)
				.orElse("Unknown Product");
		} catch (Exception e) {
			log.warn("상품명 조회 실패, 기본값 사용. productId={}", productId, e);
			return "Unknown Product";
		}
	}
}
