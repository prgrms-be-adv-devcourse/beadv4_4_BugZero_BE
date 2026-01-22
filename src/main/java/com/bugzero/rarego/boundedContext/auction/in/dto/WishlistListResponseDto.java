package com.bugzero.rarego.boundedContext.auction.in.dto;

import com.bugzero.rarego.shared.auction.dto.AuctionListResponseDto;
import lombok.Builder;

@Builder
public record WishlistListResponseDto(
        Long bookmarkId,
        AuctionListResponseDto auctionInfo
) {
    public static WishlistListResponseDto of(Long bookmarkId, AuctionListResponseDto auctionInfo) {
        return new WishlistListResponseDto(bookmarkId, auctionInfo);
    }
}