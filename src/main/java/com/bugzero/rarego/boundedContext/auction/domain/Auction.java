package com.bugzero.rarego.boundedContext.auction.domain;

import java.time.LocalDateTime;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

	private Integer currentPrice; // 초기값 null 가능

	@Column(nullable = false)
	private int tickSize;

	@Builder
	public Auction(Long productId, LocalDateTime startTime, LocalDateTime endTime, int startPrice, int tickSize) {
		this.productId = productId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startPrice = startPrice;
		this.tickSize = tickSize;
		this.status = AuctionStatus.SCHEDULED;
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
