package com.bugzero.rarego.in.dto;

import com.bugzero.rarego.shared.auction.type.AuctionStatus;

import lombok.Builder;

@Builder
public record AuctionRelistResponseDto(
	Long newAuctionId,
	Long productId,
	AuctionStatus status,
	String message
) {
}
