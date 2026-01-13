package com.bugzero.rarego.boundedContext.auction.domain;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class AuctionOrder extends BaseIdAndTime {

	@Column(nullable = false)
	private BigInteger auctionId;

	@Column(nullable = false)
	private BigInteger bidderId;

	private LocalDateTime bidTime;

	private Integer bidAmount;

	private Integer finalPrice;

	private AuctionOrderStatus status;

	@Builder
	public AuctionOrder(BigInteger auctionId, BigInteger bidderId, Integer bidAmount) {
		this.auctionId = auctionId;
		this.bidderId = bidderId;
		this.bidAmount = bidAmount;
		this.bidTime = LocalDateTime.now(); // 입찰 시각 설정
	}

}
