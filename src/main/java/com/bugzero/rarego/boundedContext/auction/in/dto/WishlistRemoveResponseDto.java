package com.bugzero.rarego.boundedContext.auction.in.dto;

public record WishlistRemoveResponseDto(
        boolean removed,
        Long bookmarkId
) {
    public static WishlistRemoveResponseDto of(boolean removed, Long bookmarkId) {
        return new WishlistRemoveResponseDto(removed, bookmarkId);
    }
}