package com.bugzero.rarego.boundedContext.auth.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.auth.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
