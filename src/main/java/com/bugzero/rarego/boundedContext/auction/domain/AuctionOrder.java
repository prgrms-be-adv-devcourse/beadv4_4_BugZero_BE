package com.bugzero.rarego.boundedContext.auction.domain;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.global.response.ErrorType;

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
@Table(name = "AUCTION_AUCTIONORDER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class AuctionOrder extends BaseIdAndTime {

	@Column(nullable = false)
	private Long auctionId;

	@Column(nullable = false)
	private Long sellerId;

	@Column(nullable = false)
	private Long bidderId;

	@Column(nullable = false)
	private int finalPrice;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuctionOrderStatus status;

	@Builder
	public AuctionOrder(Long auctionId, Long sellerId, Long bidderId, Integer finalPrice) {
		this.auctionId = auctionId;
		this.sellerId = sellerId;
		this.bidderId = bidderId;
		this.finalPrice = finalPrice;
		// 초기 상태는 결제 진행중
		this.status = AuctionOrderStatus.PROCESSING;
	}

	public void complete() {
		if (this.status != AuctionOrderStatus.PROCESSING) {
			throw new CustomException(ErrorType.INVALID_ORDER_STATUS);
		}
		this.status = AuctionOrderStatus.SUCCESS;
	}

	public void fail() {
		if (this.status != AuctionOrderStatus.PROCESSING) {
			throw new CustomException(ErrorType.INVALID_ORDER_STATUS);
		}
		this.status = AuctionOrderStatus.FAILED;
	}

	public void refund() {
		if (this.status != AuctionOrderStatus.SUCCESS) {
			throw new CustomException(ErrorType.INVALID_ORDER_STATUS);
		}
		this.status = AuctionOrderStatus.FAILED;
	}
}
