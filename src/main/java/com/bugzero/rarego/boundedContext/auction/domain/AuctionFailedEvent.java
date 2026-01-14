package com.bugzero.rarego.boundedContext.auction.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuctionFailedEvent {
    private Long auctionId;
    private Long productId;
}
