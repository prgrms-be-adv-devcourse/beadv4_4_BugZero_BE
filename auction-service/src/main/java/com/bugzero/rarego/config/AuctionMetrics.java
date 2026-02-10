package com.bugzero.rarego.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Getter
public class AuctionMetrics {

    private final Counter bidTotalCounter;
    private final Counter bidSuccessCounter;
    private final Counter bidFailNotInProgressCounter;
    private final Counter bidFailSellerBidCounter;
    private final Counter bidFailAlreadyHighestCounter;
    private final Counter bidFailAmountTooLowCounter;
    private final Counter bidFailOtherCounter;

    public AuctionMetrics(MeterRegistry registry) {
        // 입찰 시도 총 횟수
        this.bidTotalCounter = Counter.builder("auction.bid.total")
            .description("Total bid attempts")
            .register(registry);

        // 입찰 성공 횟수
        this.bidSuccessCounter = Counter.builder("auction.bid.success")
            .description("Successful bids")
            .register(registry);

        // 입찰 실패 - 경매 진행 중 아님
        this.bidFailNotInProgressCounter = Counter.builder("auction.bid.fail")
            .tag("reason", "not_in_progress")
            .description("Failed bids - auction not in progress")
            .register(registry);

        // 입찰 실패 - 판매자 본인 입찰
        this.bidFailSellerBidCounter = Counter.builder("auction.bid.fail")
            .tag("reason", "seller_bid")
            .description("Failed bids - seller cannot bid")
            .register(registry);

        // 입찰 실패 - 연속 입찰
        this.bidFailAlreadyHighestCounter = Counter.builder("auction.bid.fail")
            .tag("reason", "already_highest")
            .description("Failed bids - already highest bidder")
            .register(registry);

        // 입찰 실패 - 금액 부족
        this.bidFailAmountTooLowCounter = Counter.builder("auction.bid.fail")
            .tag("reason", "amount_too_low")
            .description("Failed bids - bid amount too low")
            .register(registry);

        // 입찰 실패 - 기타
        this.bidFailOtherCounter = Counter.builder("auction.bid.fail")
            .tag("reason", "other")
            .description("Failed bids - other reasons")
            .register(registry);
    }

    public void incrementBidTotal() {
        bidTotalCounter.increment();
    }

    public void incrementBidSuccess() {
        bidSuccessCounter.increment();
    }

    public void incrementBidFailNotInProgress() {
        bidFailNotInProgressCounter.increment();
    }

    public void incrementBidFailSellerBid() {
        bidFailSellerBidCounter.increment();
    }

    public void incrementBidFailAlreadyHighest() {
        bidFailAlreadyHighestCounter.increment();
    }

    public void incrementBidFailAmountTooLow() {
        bidFailAmountTooLowCounter.increment();
    }

    public void incrementBidFailOther() {
        bidFailOtherCounter.increment();
    }
}
