package com.bugzero.rarego.bounded_context.payment.domain;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PAYMENT_TRANSACTION")
public class PaymentTransaction extends BaseIdAndTime {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private PaymentMember member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private Wallet wallet;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WalletTransactionType transactionType;

	@Column(nullable = false)
	private int balanceDelta;

	@Column(nullable = false)
	private int holdingDelta;

	@Column(nullable = false)
	private int balanceAfter;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReferenceType referenceType;

	@Column(nullable = false)
	private Long referenceId;
}
