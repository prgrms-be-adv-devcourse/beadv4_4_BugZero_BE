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
	// TODO: 수수료율은 팀원들과 합의 후 확정 필요
	private static final double FEE_RATE = 0.1; // 10% 수수료

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

	public void fail() {
		this.status = SettlementStatus.FAILED;
	}
}
