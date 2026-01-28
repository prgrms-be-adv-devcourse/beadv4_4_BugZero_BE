package com.bugzero.rarego.shared.auction.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;

public record MyBidResponseDto(
	Long bidId,
	Long auctionId,
	Long productId,
	long bidAmount,
	LocalDateTime bidTime,
	AuctionStatus auctionStatus,
	long currentPrice,
	LocalDateTime endTime
) {
	public static MyBidResponseDto from(Bid bid, Auction auction) {
		return new MyBidResponseDto(
			bid.getId(),
			auction.getId(),
			auction.getProductId(),
			bid.getBidAmount(),
			bid.getBidTime(),
			auction.getStatus(),
			auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartPrice(),
			auction.getEndTime()
		);
	}
}
