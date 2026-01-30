package com.bugzero.rarego.bounded_context.auction.in.dto;

public record AuctionRemoveBookmarkResponseDto(
        boolean removed,
        Long bookmarkId
) {
    public static AuctionRemoveBookmarkResponseDto of(boolean removed, Long bookmarkId) {
        return new AuctionRemoveBookmarkResponseDto(removed, bookmarkId);
    }
}