package com.bugzero.rarego.boundedContext.payment.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@Table(name = "PAYMENT_SETTLEMENT_FEE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementFee extends BaseIdAndTime {
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false, unique = true)
	private Settlement settlement;

	@Column(nullable = false)
	private int feeAmount;
}
