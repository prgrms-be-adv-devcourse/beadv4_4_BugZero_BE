package com.bugzero.rarego.bounded_context.auction.domain.event;

import java.time.LocalDateTime;

/**
 * 경매 정보 변경 시 발행되는 이벤트
 *
 * @param auctionId  경매 ID
 * @param oldEndTime 기존 종료 시간
 * @param newEndTime 변경된 종료 시간
 */
public record AuctionUpdatedEvent(
        Long auctionId,
        LocalDateTime oldEndTime,
        LocalDateTime newEndTime
) {
}
