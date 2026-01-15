package com.bugzero.rarego.shared.auction.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.auction.domain.Bid;

public record BidLogResponseDto(
	Long id,
	String publicId,
	LocalDateTime bidTime,
	long bidAmount
) {
	public static BidLogResponseDto from(Bid bid) {
		return new BidLogResponseDto(
			bid.getId(),
			bid.getBidder().getPublicId(),
			bid.getBidTime(),
			bid.getBidAmount()
		);
	}
}
