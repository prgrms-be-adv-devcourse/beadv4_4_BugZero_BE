package com.bugzero.rarego.boundedContext.auction.domain;

import java.time.LocalDateTime;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AUCTION_BID")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Bid extends BaseIdAndTime {

	@Column(nullable = false)
	private Long auctionId;

	@Column(nullable = false)
	private Long bidderId;

	@Column(nullable = false)
	private LocalDateTime bidTime;

	@Column(nullable = false)
	private int bidAmount;

	@Builder
	public Bid(Long auctionId, Long bidderId, int bidAmount, LocalDateTime bidTime) {
		this.auctionId = auctionId;
		this.bidderId = bidderId;
		this.bidAmount = bidAmount;
		// 입찰시간을 인자로 받기 위해서 if문 처리
		this.bidTime = bidTime != null ? bidTime : LocalDateTime.now();
	}
}
