package com.bugzero.rarego.boundedContext.auction.in.dto;

public record AuctionAddBookmarkResponseDto(
        boolean bookmarked,
        Long auctionId
) {
    public static AuctionAddBookmarkResponseDto of(boolean bookmarked, Long auctionId) {
        return new AuctionAddBookmarkResponseDto(bookmarked, auctionId);
    }
}
