package com.bugzero.rarego.shared.auction.dto;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionDetailResponseDto(
	Long auctionId,
	Long productId,
	String productName,
	String productDescription,
	List<String> imageUrls,
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
		boolean isMyHighestBid,
		boolean isSeller
	) {}

	public record MyParticipationInfo(
		boolean hasBid,
		Integer myLastBidPrice
	) {}

	public static AuctionDetailResponseDto from(
		Auction auction,
		String productName,
		String productDescription,
		List<String> imageUrls,
		Bid highestBid,
		Bid myLastBid,
		Long currentMemberId
	) {
		LocalDateTime now = LocalDateTime.now();

		long remainingSeconds = 0;
		if (auction.getEndTime() != null && auction.getEndTime().isAfter(now)) {
			remainingSeconds = Duration.between(now, auction.getEndTime()).getSeconds();
		}

		int currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartPrice();

		PriceInfo priceInfo = new PriceInfo(
			auction.getStartPrice(),
			currentPrice,
			auction.getTickSize());

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
			&& auction.getEndTime() != null
			&& auction.getEndTime().isAfter(now)
			&& !isGuest
			&& !isSeller
			&& !isMyHighestBid;

		// 첫 입찰인 경우(highestBid == null) 시작가(currentPrice)로 입찰 가능
		int minBidPrice = (highestBid == null) ? currentPrice : currentPrice + auction.getTickSize();
		Long highestBidderId = (highestBid != null) ? highestBid.getBidderId() : null;

		BidInfo bidInfo = new BidInfo(
			canBid,
			minBidPrice,
			highestBidderId,
			isMyHighestBid,
			isSeller);

		MyParticipationInfo myParticipationInfo = new MyParticipationInfo(
			hasBid,
			myLastBidPrice
		);

		return new AuctionDetailResponseDto(
			auction.getId(),
			auction.getProductId(),
			productName,
			productDescription,
			imageUrls,
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