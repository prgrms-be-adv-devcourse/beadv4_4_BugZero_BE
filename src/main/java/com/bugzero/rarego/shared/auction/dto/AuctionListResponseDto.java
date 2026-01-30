package com.bugzero.rarego.shared.auction.dto;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;

import java.time.LocalDateTime;

public record AuctionListResponseDto(
	Long auctionId,
	Long productId,
	String productName,
	String thumbnailUrl,
	String category,
	int currentPrice,
	int startPrice,
	int bidsCount,         // 입찰 수
	AuctionStatus auctionStatus,
	LocalDateTime endTime
) {
	public static AuctionListResponseDto from(
		Auction auction,
		ProductAuctionResponseDto product,
		String thumbnailUrl,
		int bidsCount
	) {
		int safeCurrentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartPrice();

		return new AuctionListResponseDto(
			auction.getId(),
			auction.getProductId(),
			// NPE 방지
			product != null ? product.name() : "Unknown Product",
			thumbnailUrl,
			// NPE 방지
			product != null ? product.category() : "ETC",

			safeCurrentPrice,
			auction.getStartPrice(),
			bidsCount,
			auction.getStatus(),
			auction.getEndTime()
		);
	}
}
