package com.bugzero.rarego.boundedContext.auction.domain.event;

/**
 * 경매 실패(유찰) 시 발행되는 이벤트
 *
 * @param auctionId 경매 ID
 * @param productId 상품 ID
 */
public record AuctionFailedEvent(
        Long auctionId,
        Long productId
) {
}
