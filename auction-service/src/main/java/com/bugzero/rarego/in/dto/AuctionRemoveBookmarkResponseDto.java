package com.bugzero.rarego.in.dto;

public record AuctionRemoveBookmarkResponseDto(
	boolean removed,
	Long auctionId) {
	public static AuctionRemoveBookmarkResponseDto of(boolean removed, Long auctionId) {
		return new AuctionRemoveBookmarkResponseDto(removed, auctionId);
	}
}
