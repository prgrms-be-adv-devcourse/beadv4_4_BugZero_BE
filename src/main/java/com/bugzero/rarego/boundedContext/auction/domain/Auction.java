package com.bugzero.rarego.boundedContext.auction.domain;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.global.response.ErrorType;
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
    private Long sellerId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    @Column(nullable = false)
    private int startPrice;

    private Integer currentPrice; // 초기값 null 가능

    @Column(nullable = false)
    private int tickSize;

    // 입찰 가격 갱신
    @Builder
    public Auction(Long productId, Long sellerId, LocalDateTime startTime, LocalDateTime endTime, int startPrice,
            int tickSize) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startPrice = startPrice;
        this.tickSize = tickSize;
        this.status = AuctionStatus.SCHEDULED;
    }

    public void start() {
        if (this.status != AuctionStatus.SCHEDULED) {
            throw new CustomException(ErrorType.AUCTION_NOT_SCHEDULED);
        }
        this.status = AuctionStatus.IN_PROGRESS;
    }

    public void end() {
        if (this.status != AuctionStatus.IN_PROGRESS) {
            throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS);
        }
        this.status = AuctionStatus.ENDED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.endTime);
    }

    public void forceStartForTest() {
        // 테스트/로컬 초기화 전용
        this.status = AuctionStatus.IN_PROGRESS;
    }

    // 입찰 가격 갱신
    public void updateCurrentPrice(int price) {
      this.currentPrice = price;
    }

    // 경매 시작 상태로 전이
    public void startAuction() {
      this.status = AuctionStatus.IN_PROGRESS;
    }

}
