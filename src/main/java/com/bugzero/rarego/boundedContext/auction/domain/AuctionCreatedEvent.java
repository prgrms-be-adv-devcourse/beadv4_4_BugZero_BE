package com.bugzero.rarego.boundedContext.auction.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuctionCreatedEvent {
    private final Long auctionId;
    private final LocalDateTime endTime;
}