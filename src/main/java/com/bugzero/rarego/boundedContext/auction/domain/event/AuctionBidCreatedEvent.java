package com.bugzero.rarego.boundedContext.auction.domain.event;

import java.time.LocalDateTime;

/**
 * 입찰 생성 이벤트
 * SSE 브로드캐스트를 위해 발행됨
 */
public record AuctionBidCreatedEvent(
        Long auctionId,
        Long bidderId,
        Integer bidAmount,
        LocalDateTime bidTime
) {
    public static AuctionBidCreatedEvent of(Long auctionId, Long bidderId, Integer bidAmount) {
        return new AuctionBidCreatedEvent(
                auctionId,
                bidderId,
                bidAmount,
                LocalDateTime.now()
        );
    }
}