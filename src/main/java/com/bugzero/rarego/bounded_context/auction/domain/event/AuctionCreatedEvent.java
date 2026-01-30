package com.bugzero.rarego.bounded_context.auction.domain.event;

import java.time.LocalDateTime;

/**
 * 경매 생성 시 발행되는 이벤트
 *
 * @param auctionId 경매 ID
 * @param endTime   경매 종료 시간
 */
public record AuctionCreatedEvent(
        Long auctionId,
        LocalDateTime endTime
) {
}
