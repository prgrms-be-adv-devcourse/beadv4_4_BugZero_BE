package com.bugzero.rarego.shared.auction.dto;

import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import java.time.LocalDateTime;

public record BidResponseDto(
	Long bidId,
	Long auctionId,
	String publicId,         // 입찰자 공개 ID(UUID)
	LocalDateTime bidTime,   // 입찰 시각
	Long bidAmount,          // 입찰 금액
	Long updatedCurrentPrice // 갱신된 현재가
) {
	// Entity -> DTO 변환 메서드
	public static BidResponseDto from(Bid bid, String publicId, Long updatedCurrentPrice) {
		return new BidResponseDto(
			bid.getId(),
			bid.getAuctionId(),
			publicId,
			bid.getBidTime(),
			Long.valueOf(bid.getBidAmount()),
			updatedCurrentPrice
		);
	}
}