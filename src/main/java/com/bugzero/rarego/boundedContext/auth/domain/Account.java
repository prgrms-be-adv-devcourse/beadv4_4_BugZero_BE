package com.bugzero.rarego.boundedContext.auth.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Account extends BaseIdAndTime{

	// TODO: member와 연관관계 publicId로 변경
	@Column(name = "member_public_id", nullable = false)
	private Long memberPublicId;

	@Enumerated(EnumType.STRING)
	@Column(name = "auth_role", nullable = false)
	private AuthRole authRole;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false)
	private Provider provider;

	@Column(name = "provider_id", length = 128, nullable = false)
	private String providerId;
}
