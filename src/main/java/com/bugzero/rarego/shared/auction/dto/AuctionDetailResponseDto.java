package com.bugzero.rarego.shared.auction.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;

public record AuctionDetailResponseDto (
	Long id,
	Long productId,
	LocalDateTime startTime,
	LocalDateTime endTime,
	AuctionStatus auctionStatus,
	int startPrice,
	Integer currentPrice,
	int tickSize
) {
	public static AuctionDetailResponseDto from(Auction auction) {

		return new  AuctionDetailResponseDto(
			auction.getId(),
			auction.getProductId(),
			auction.getStartTime(),
			auction.getEndTime(),
			auction.getStatus(),
			auction.getStartPrice(),
			auction.getCurrentPrice(),
			auction.getTickSize()
		);
	}

}
