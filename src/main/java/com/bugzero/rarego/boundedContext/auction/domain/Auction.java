package com.bugzero.rarego.boundedContext.auction.domain;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Auction extends BaseIdAndTime {

	@Column(nullable = false)
	private Long productId;

	private LocalDateTime startTime;

	private LocalDateTime endTime;

	@Enumerated(EnumType.STRING)
	private AuctionStatus status;

	private int startPrice;

	private int currentPrice;

	private int tickSize;

	@Version // 낙관적 락
	private Long version;

	@Builder
	public Auction(Long productId, LocalDateTime startTime, LocalDateTime endTime, int startPrice, int tickSize) {
		this.productId = productId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startPrice = startPrice;
		this.tickSize = tickSize;
		this.status = AuctionStatus.SCHEDULED; // 기본값 설정 용이
	}

	// 입찰 가격 갱신
}
