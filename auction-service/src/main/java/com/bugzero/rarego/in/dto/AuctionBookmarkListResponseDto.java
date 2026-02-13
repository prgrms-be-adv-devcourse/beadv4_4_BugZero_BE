package com.bugzero.rarego.in.dto;

import lombok.Builder;

@Builder
public record AuctionBookmarkListResponseDto(
	Long bookmarkId,
	AuctionListResponseDto auctionInfo
) {
	public static AuctionBookmarkListResponseDto of(Long bookmarkId, AuctionListResponseDto auctionInfo) {
		return new AuctionBookmarkListResponseDto(bookmarkId, auctionInfo);
	}
}
