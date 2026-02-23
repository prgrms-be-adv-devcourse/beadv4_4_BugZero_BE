package com.bugzero.rarego.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;

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

	private Integer currentPrice;

	@Column(nullable = false)
	private int tickSize;

	@Builder
	public Auction(Long productId, Long sellerId, LocalDateTime startTime, Integer durationDays, LocalDateTime endTime,
		int startPrice) {
		this.productId = productId;
		this.sellerId = sellerId;
		this.startTime = startTime;
		this.durationDays = durationDays;
		this.endTime = endTime;
		this.startPrice = startPrice;
		this.currentPrice = startPrice;
		this.tickSize = AuctionTickPolicy.resolveTickSize(startPrice);
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
		this.status = AuctionStatus.IN_PROGRESS;
	}

	public void updateCurrentPrice(int price) {
		if (this.currentPrice == null || price > this.currentPrice) {
			this.currentPrice = price;
		}
	}

	public boolean hasStartTime() {
		return this.startTime != null;
	}

	public void determineStart(LocalDateTime startTime) {
		this.startTime = startTime;
		this.endTime = startTime.plusDays(this.durationDays);
	}

	public boolean isSeller(Long sellerId) {
		return Objects.equals(this.sellerId, sellerId);
	}

	public void update(int durationDays, int startPrice) {
		this.durationDays = durationDays;
		this.startPrice = startPrice;
		this.tickSize = AuctionTickPolicy.resolveTickSize(startPrice);
	}

	public void withdraw() {
		this.status = AuctionStatus.WITHDRAWN;
	}

	public Integer getCurrentPriceOrStartPrice() {
		return currentPrice != null ? currentPrice : startPrice;
	}

	public boolean extendEndTimeIfClose(LocalDateTime now) {
		long minutesRemaining = ChronoUnit.MINUTES.between(now, this.endTime);

		if (minutesRemaining >= 0 && minutesRemaining <= 3) {
			this.endTime = this.endTime.plusMinutes(3);
			return true;
		}
		return false;
	}
}
