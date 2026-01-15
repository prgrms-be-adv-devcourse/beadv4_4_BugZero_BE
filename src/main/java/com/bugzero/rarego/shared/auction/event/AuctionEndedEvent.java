package com.bugzero.rarego.shared.auction.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuctionEndedEvent {
    private Long auctionId;
    private Long winnerId;
    private Integer finalPrice;
    private Long productId;
}
