package com.bugzero.rarego.boundedContext.payment.domain;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.global.response.ErrorType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PAYMENT_DEPOSIT", indexes = {
		@Index(name = "idx_deposit_auction_id", columnList = "auction_id")
}, uniqueConstraints = {
		@UniqueConstraint(name = "uk_deposit_member_auction", columnNames = { "member_id", "auction_id" }) // 한 회원은 한
																											// 경매에 중복 예치
																											// 불가
})
public class Deposit extends BaseIdAndTime {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private PaymentMember member;

	@Column(nullable = false)
	private Long auctionId;

	@Column(nullable = false)
	private int amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DepositStatus status;

	public static Deposit create(PaymentMember member, Long auctionId, int amount) {
		return Deposit.builder()
				.member(member)
				.auctionId(auctionId)
				.amount(amount)
				.status(DepositStatus.HOLD)
				.build();
	}

	public void release() {
		this.status = DepositStatus.RELEASED;
	}

	public void use() {
		if (this.status != DepositStatus.HOLD) {
			throw new CustomException(ErrorType.ALREADY_USED_DEPOSIT);
		}
		this.status = DepositStatus.USED;
	}

	public void forfeit() {
		if (this.status != DepositStatus.HOLD) {
			throw new CustomException(ErrorType.ALREADY_USED_DEPOSIT);
		}
		this.status = DepositStatus.FORFEITED;
	}
}
