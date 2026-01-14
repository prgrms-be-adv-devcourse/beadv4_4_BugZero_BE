package com.bugzero.rarego.boundedContext.payment.domain;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.global.response.ErrorType;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PAYMENT_WALLET")
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseIdAndTime {
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false, unique = true)
	private PaymentMember member;

	@Builder.Default
	private int balance = 0;

	@Builder.Default
	private int holdingAmount = 0;

	@Version
	private int version;

	public void hold(int amount) {
		if (balance - holdingAmount < amount) {
			throw new CustomException(ErrorType.INSUFFICIENT_BALANCE);
		}
		this.holdingAmount += amount;
	}

	public void release(int amount) {
		if (holdingAmount < amount) {
			throw new CustomException(ErrorType.INSUFFICIENT_HOLDING);
		}
		this.holdingAmount -= amount;
	}
}
