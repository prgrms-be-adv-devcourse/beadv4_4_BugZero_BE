package com.bugzero.rarego.boundedContext.auction.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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

    @Column(nullable = true)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private int durationDays;

    @Column(nullable = true)
    private LocalDateTime endTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    @Column(nullable = false)
    private int startPrice;

    private Integer currentPrice; // 초기값 null 가능

    @Column(nullable = false)
    private int tickSize;

    // 연장 횟수 카운트
    @Column(nullable = false)
    private int extensionCount = 0;

    // 입찰 가격 갱신
    @Builder
    public Auction(Long productId, Long sellerId, LocalDateTime startTime,  Integer durationDays, LocalDateTime endTime, int startPrice) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.startTime = startTime;
        this.durationDays = durationDays;
        this.endTime = endTime;
        this.startPrice = startPrice;
        this.currentPrice = startPrice; // 초기 현재가는 시작가와 동일하게 설정
        this.tickSize = determineTickSize(startPrice);
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
        if (this.currentPrice == null || price > this.currentPrice) {
            this.currentPrice = price;
        }
    }

    // 시작 시간 유무에 따라 경매예정이 확정되었는지 확인
    public boolean hasStartTime() {
        return this.startTime != null;
    }

    public void determineStart(LocalDateTime startTime) {
        this.startTime = startTime;
        this.endTime = startTime.plusDays(this.durationDays);
    }

    // 접근 회원이 경매 판매자인지 확인
    public boolean isSeller(Long sellerId) {
        return Objects.equals(this.sellerId, sellerId);
    }

    public void update(int durationDays, int startPrice) {
        this.durationDays = durationDays;
        this.startPrice = startPrice;
        this.tickSize = determineTickSize(startPrice);
    }

    public void withdraw() {
        this.status = AuctionStatus.WITHDRAWN;
    }

    // 호가단위 결정
    private int determineTickSize(int startPrice) {
        if (startPrice < 10000) {
            return 500;
        } else if (startPrice < 50000) {
            return 1000;
        } else if (startPrice < 100000) {
            return 2000;
        } else if (startPrice < 300000) {
            return 5000;
        } else if (startPrice < 1000000) {
            return 10000;
        } else {
            return 30000; // 100만 원 이상
        }
    }

    public boolean extendEndTimeIfClose(LocalDateTime now) {
        // 종료 시간까지 남은 분(minute) 계산
        long minutesRemaining = ChronoUnit.MINUTES.between(now, this.endTime);

        // 1. 남은 시간이 0분 이상 3분 이하인지 확인
        // 2. 연장 횟수가 5회 미만인지 확인 (총 5회)
        if (minutesRemaining >= 0 && minutesRemaining <= 3 && this.extensionCount < 5) {
            this.endTime = this.endTime.plusMinutes(3); // 3분 연장
            this.extensionCount++;
            return true;
        }
        return false;
    }

    // 연장 관련 초기 데이터 확인을 위한 카운트 확인
    public void setExtensionCountForTest(int count) {
        this.extensionCount = count;
    }

}
