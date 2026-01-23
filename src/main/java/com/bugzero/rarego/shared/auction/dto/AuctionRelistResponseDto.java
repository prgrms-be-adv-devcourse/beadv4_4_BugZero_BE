package com.bugzero.rarego.shared.auction.dto;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;

import lombok.Builder;

@Builder
public record AuctionRelistResponseDto (
	Long newAuctionId,
	Long productId,
	AuctionStatus status,
	String message
) { }
