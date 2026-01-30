package com.bugzero.rarego.boundedContext.auction.in.dto;

public record AuctionRemoveBookmarkResponseDto(
        boolean removed,
        Long auctionId) {
    public static AuctionRemoveBookmarkResponseDto of(boolean removed, Long auctionId) {
        return new AuctionRemoveBookmarkResponseDto(removed, auctionId);
    }
}