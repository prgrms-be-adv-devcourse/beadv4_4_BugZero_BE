package com.bugzero.rarego.bounded_context.auth.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.bounded_context.auth.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
