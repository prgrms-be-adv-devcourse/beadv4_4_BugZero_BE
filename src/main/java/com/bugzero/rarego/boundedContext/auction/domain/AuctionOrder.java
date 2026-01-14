package com.bugzero.rarego.boundedContext.auction.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "AUCTION_AUCTIONORDER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class AuctionOrder extends BaseIdAndTime {

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long bidderId;

    @Column(nullable = false)
    private int finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionOrderStatus status;

    @Builder
    public AuctionOrder(Long auctionId, Long bidderId, Integer finalPrice) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.finalPrice = finalPrice;
        // 초기 상태는 결제 진행중
        this.status = AuctionOrderStatus.PROCESSING;
    }

}
