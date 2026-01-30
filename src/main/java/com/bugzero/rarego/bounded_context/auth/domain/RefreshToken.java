package com.bugzero.rarego.bounded_context.auth.domain;

import java.time.LocalDateTime;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
	@Table(name = "AUTH_REFRESH_TOKEN",
	indexes = {
		@Index(name = "idx_auth_refresh_token_member_public_id", columnList = "member_public_id"),
		@Index(name = "idx_auth_refresh_token_revoked_expires", columnList = "revoked, expires_at")
	}
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseIdAndTime {

	@Column(name = "member_public_id", nullable = false)
	private Long memberPublicId;

	@Column(name = "refresh_token_hash", nullable = false, length = 255)
	private String refreshTokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "revoked", nullable = false)
	private boolean revoked = false;

	public void revoke() {
		this.revoked = true;
	}

	public boolean isExpired(LocalDateTime now) {
		return expiresAt.isBefore(now);
	}
}
