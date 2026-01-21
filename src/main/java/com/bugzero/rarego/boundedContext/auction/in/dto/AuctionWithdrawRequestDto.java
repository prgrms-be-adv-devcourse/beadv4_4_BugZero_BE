package com.bugzero.rarego.boundedContext.auction.in.dto;

public record AuctionWithdrawRequestDto(
        String reason  // 선택적: 포기 사유
) {
}