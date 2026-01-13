package com.bugzero.rarego.boundedContext.auction.domain;

import java.math.BigInteger;

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
public class AuctionBookmark extends BaseIdAndTime {

	@Column(nullable = false)
	private BigInteger auctionId;

	@Column(nullable = false)
	private BigInteger memberId;

	@Column(nullable = false)
	private BigInteger productId;

	@Builder
	public AuctionBookmark(BigInteger auctionId, BigInteger memberId, BigInteger productId) {
		this.auctionId = auctionId;
		this.memberId = memberId;
		this.productId = productId;
	}

}
