package com.bugzero.rarego.boundedContext.auction.in.dto;

import com.bugzero.rarego.shared.auction.dto.AuctionListResponseDto;
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