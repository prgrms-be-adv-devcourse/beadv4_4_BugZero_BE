package com.bugzero.rarego.bounded_context.auction.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AUCTION_AUCTIONBOOKMARK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class AuctionBookmark extends BaseIdAndTime {

	@Column(nullable = false)
	private Long auctionId;

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false)
	private Long productId;

	@Builder
	public AuctionBookmark(Long auctionId, Long memberId, Long productId) {
		this.auctionId = auctionId;
		this.memberId = memberId;
		this.productId = productId;
	}

}
