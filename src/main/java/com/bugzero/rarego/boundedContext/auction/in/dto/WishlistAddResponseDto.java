package com.bugzero.rarego.boundedContext.auction.in.dto;

public record WishlistAddResponseDto(
        boolean bookmarked,
        Long auctionId
) {
    public static WishlistAddResponseDto of(boolean bookmarked, Long auctionId) {
        return new WishlistAddResponseDto(bookmarked, auctionId);
    }
}
