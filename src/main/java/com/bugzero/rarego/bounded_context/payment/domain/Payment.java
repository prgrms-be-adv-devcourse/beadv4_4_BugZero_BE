package com.bugzero.rarego.bounded_context.payment.domain;

import java.util.UUID;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.global.response.ErrorType;

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
@Getter
@Builder
@Table(name = "PAYMENT_PAYMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseIdAndTime {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private PaymentMember member;

	@Column(nullable = false, unique = true, length = 100)
	private String orderId;

	@Column(nullable = false)
	private int amount;

	private String paymentKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status;

	public static Payment of(PaymentMember member, int amount) {
		return Payment.builder()
			.member(member)
			.orderId(UUID.randomUUID().toString())
			.amount(amount)
			.status(PaymentStatus.PENDING)
			.build();
	}

	public void complete(String paymentKey) {
		this.paymentKey = paymentKey;
		this.status = PaymentStatus.DONE;
	}

	public void fail() {
		this.status = PaymentStatus.FAILED;
	}

	public void validate(Long memberId, int amount) {
		if (!this.member.getId().equals(memberId)) {
			throw new CustomException(ErrorType.PAYMENT_OWNER_MISMATCH);
		}

		if (this.amount != amount) {
			throw new CustomException(ErrorType.INVALID_PAYMENT_AMOUNT);
		}

		if (this.status != PaymentStatus.PENDING) {
			throw new CustomException(ErrorType.ALREADY_PROCESSED_PAYMENT);
		}
	}
}
