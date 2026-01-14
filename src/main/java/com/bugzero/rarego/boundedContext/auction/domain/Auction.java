package com.bugzero.rarego.boundedContext.auction.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUCTION_AUCTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Auction extends BaseIdAndTime {

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    @Column(nullable = false)
    private int startPrice;

    private int currentPrice; // 초기값 null 가능

    @Column(nullable = false)
    private int tickSize;

    // 입찰 가격 갱신
    @Builder
    public Auction(Long productId, LocalDateTime startTime, LocalDateTime endTime, int startPrice, int tickSize) {
        this.productId = productId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startPrice = startPrice;
        this.tickSize = tickSize;
        this.status = AuctionStatus.SCHEDULED;
    }

    public void end() {
        if (this.status != AuctionStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 경매만 종료할 수 있습니다.");
        }
        this.status = AuctionStatus.ENDED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.endTime);
    }
}
