package com.bugzero.rarego.shared.auction.dto;

import com.bugzero.rarego.bounded_context.auction.domain.Auction;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionStatus;
import com.bugzero.rarego.bounded_context.auction.domain.Bid;
import java.time.Duration;
import java.time.LocalDateTime;

public record AuctionDetailResponseDto(
	Long auctionId,
	Long productId,
	AuctionStatus status,
	LocalDateTime startTime,
	LocalDateTime endTime,
	long remainingSeconds,
	PriceInfo price,
	BidInfo bid,
	MyParticipationInfo myParticipation
) {
	public record PriceInfo(
		int startPrice,
		int currentPrice,
		int tickSize
	) {}

	public record BidInfo(
		boolean canBid,
		int minBidPrice,
		Long highestBidderId,
		boolean isMyHighestBid
	) {}

	public record MyParticipationInfo(
		boolean hasBid,
		Integer myLastBidPrice
	) {}

	public static AuctionDetailResponseDto from(
		Auction auction,
		Bid highestBid,
		Bid myLastBid,
		Long currentMemberId
	) {
		LocalDateTime now = LocalDateTime.now();

		long remainingSeconds = 0;
		if (auction.getEndTime().isAfter(now)) {
			remainingSeconds = Duration.between(now, auction.getEndTime()).getSeconds();
		}

		PriceInfo priceInfo = new PriceInfo(
			auction.getStartPrice(),
			auction.getCurrentPrice(),
			auction.getTickSize()
		);

		boolean isMyHighestBid = false;
		boolean hasBid = false;
		Integer myLastBidPrice = null;

		// 로그인한 사용자라면 실제 값 판별
		if (currentMemberId != null) {
			if (highestBid != null && highestBid.getBidderId().equals(currentMemberId)) {
				isMyHighestBid = true;
			}

			// 내 입찰 기록이 있는지 확인
			if (myLastBid != null) {
				hasBid = true;
				myLastBidPrice = myLastBid.getBidAmount();
			}
		}

		boolean isGuest = (currentMemberId == null);
		boolean isSeller = (currentMemberId != null) && auction.getSellerId().equals(currentMemberId);

		boolean canBid = auction.getStatus() == AuctionStatus.IN_PROGRESS
			&& auction.getEndTime().isAfter(now)
			&& !isGuest
			&& !isSeller
			&& !isMyHighestBid;

		int minBidPrice = auction.getCurrentPrice() + auction.getTickSize();
		Long highestBidderId = (highestBid != null) ? highestBid.getBidderId() : null;

		BidInfo bidInfo = new BidInfo(
			canBid,
			minBidPrice,
			highestBidderId,
			isMyHighestBid
		);

		MyParticipationInfo myParticipationInfo = new MyParticipationInfo(
			hasBid,
			myLastBidPrice
		);

		return new AuctionDetailResponseDto(
			auction.getId(),
			auction.getProductId(),
			auction.getStatus(),
			auction.getStartTime(),
			auction.getEndTime(),
			remainingSeconds,
			priceInfo,
			bidInfo,
			myParticipationInfo
		);
	}
}