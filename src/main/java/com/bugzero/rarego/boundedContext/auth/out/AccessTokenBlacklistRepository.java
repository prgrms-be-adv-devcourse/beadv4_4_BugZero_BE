package com.bugzero.rarego.boundedContext.auth.out;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.auth.domain.AccessTokenBlacklist;

public interface AccessTokenBlacklistRepository extends JpaRepository<AccessTokenBlacklist, Long> {
	boolean existsByAccessTokenAndExpiresAtAfter(String accessTokenHash, LocalDateTime now);

	long deleteByExpiresAtBefore(LocalDateTime now);
}
