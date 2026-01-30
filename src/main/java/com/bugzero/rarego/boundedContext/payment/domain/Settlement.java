package com.bugzero.rarego.boundedContext.payment.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@Table(name = "PAYMENT_SETTLEMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseIdAndTime {
	private static final double FEE_RATE = 0.1; // 10% 수수료

	private static final int MAX_TRY_COUNT = 3;

	@Column(nullable = false, unique = true)
	private Long auctionId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private PaymentMember seller;

	@Column(nullable = false)
	private int salesAmount;

	@Column(nullable = false)
	private int feeAmount;

	@Column(nullable = false)
	private int settlementAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private SettlementStatus status = SettlementStatus.READY;

	@Builder.Default
	@Column(nullable = false)
	private int tryCount = 0;

	public static Settlement create(Long auctionId, PaymentMember seller, int salesAmount) {
		int feeAmount = (int)(salesAmount * FEE_RATE);
		int settlementAmount = salesAmount - feeAmount;

		return Settlement.builder()
			.auctionId(auctionId)
			.seller(seller)
			.salesAmount(salesAmount)
			.feeAmount(feeAmount)
			.settlementAmount(settlementAmount)
			.status(SettlementStatus.READY)
			.build();
	}

	public void complete() {
		this.status = SettlementStatus.DONE;
	}

	// 정산 중 실패 처리
	public boolean fail() {
		this.tryCount++;

		// 3번 시도해도 실패하면 FAILED로 변경
		if (this.tryCount > MAX_TRY_COUNT) {
			this.status = SettlementStatus.FAILED;
			return true; // 수동 처리 대상
		}

		// 재처리 대상 다음 배치 때 재시도
		return false;
	}

	// 환불 시 실패 처리
	public void cancel() {
		this.status = SettlementStatus.CANCELED;
	}

	public static Settlement createFromForfeit(Long auctionId, PaymentMember seller, int forfeitAmount) {
		return Settlement.builder()
			.auctionId(auctionId)
			.seller(seller)
			.salesAmount(forfeitAmount)
			.feeAmount(0)
			.settlementAmount(forfeitAmount)
			.status(SettlementStatus.READY)
			.build();
	}
}
