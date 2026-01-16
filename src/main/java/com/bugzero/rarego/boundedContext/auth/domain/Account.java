package com.bugzero.rarego.boundedContext.auth.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "AUTH_ACCOUNT",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_auth_account_provider_provider_id",
			columnNames = {"provider", "provider_id"}
		)
	}
)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseIdAndTime {

	@Column(name = "member_public_id", nullable = false, unique = true, length = 36)
	private String memberPublicId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private AuthRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false)
	private Provider provider;

	@Column(name = "provider_id", length = 128, nullable = false)
	private String providerId;

	public static Account toEntity(
		String memberPublicId,
		AuthRole role,
		Provider provider,
		String providerId
	) {
		return Account.builder()
			.memberPublicId(memberPublicId)
			.role(role)
			.provider(provider)
			.providerId(providerId)
			.build();
	}
}
