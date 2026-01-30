package com.bugzero.rarego.shared.auction.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.bounded_context.auction.domain.Bid;

public record BidLogResponseDto(
	Long id,
	String publicId,
	LocalDateTime bidTime,
	long bidAmount
) {
	public static BidLogResponseDto from(Bid bid, String publicId) {
		return new BidLogResponseDto(
			bid.getId(),
			publicId,
			bid.getBidTime(),
			bid.getBidAmount()
		);
	}
}
