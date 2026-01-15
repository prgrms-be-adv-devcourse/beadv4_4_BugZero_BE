package com.bugzero.rarego.boundedContext.auction.domain;

import java.time.LocalDateTime;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Bid extends BaseIdAndTime {

	// join(Auction)을 효율적으로 수행하기 위해 auctionId 필드를 객체 연관관계로 변경.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auction_id", nullable = false)
	private Auction auction;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bidder_id", nullable = false)
	private AuctionMember bidder;

	@Column(nullable = false)
	private LocalDateTime bidTime;

	@Column(nullable = false)
	private int bidAmount;

	@Builder
	public Bid(Auction auction, AuctionMember bidder, int bidAmount) {
		this.auction = auction;
		this.bidder = bidder;
		this.bidAmount = bidAmount;
		this.bidTime = LocalDateTime.now();
	}
}
