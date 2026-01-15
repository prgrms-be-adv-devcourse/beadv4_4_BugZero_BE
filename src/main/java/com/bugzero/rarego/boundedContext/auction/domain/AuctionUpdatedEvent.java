package com.bugzero.rarego.boundedContext.auction.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuctionUpdatedEvent {
    private final Long auctionId;
    private final LocalDateTime oldEndTime;
    private final LocalDateTime newEndTime;
}