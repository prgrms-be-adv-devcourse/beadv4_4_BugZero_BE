package com.bugzero.rarego.boundedContext.auction.in.dto;

public record WishlistRemoveResponseDto(
        boolean removed,
        Long auctionId) {
    public static WishlistRemoveResponseDto of(boolean removed, Long auctionId) {
        return new WishlistRemoveResponseDto(removed, auctionId);
    }
}