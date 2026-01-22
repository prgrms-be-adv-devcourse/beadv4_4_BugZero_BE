package com.bugzero.rarego.boundedContext.auth.domain;

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
@Table(name = "AUTH_ACCESS_TOKEN_BLACKLIST",
	indexes = {
		@Index(name = "idx_auth_access_token_blacklist_token", columnList = "access_token"),
		@Index(name = "idx_auth_access_token_blacklist_expires", columnList = "expires_at")
	}
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessTokenBlacklist extends BaseIdAndTime {

	@Column(name = "access_token", nullable = false, length = 512)
	private String accessToken;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;
}
