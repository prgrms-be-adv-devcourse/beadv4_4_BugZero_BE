package com.bugzero.rarego.boundedContext.auction.in.dto;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;

import java.time.LocalDateTime;

public record WishlistListResponseDto(
        Long bookmarkId,
        Long auctionId,
        Long productId,
        AuctionStatus auctionStatus,
        Integer currentPrice,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public static WishlistListResponseDto of(
            Long bookmarkId,
            Long auctionId,
            Long productId,
            AuctionStatus auctionStatus,
            Integer currentPrice,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return new WishlistListResponseDto(
                bookmarkId,
                auctionId,
                productId,
                auctionStatus,
                currentPrice,
                startTime,
                endTime
        );
    }
}