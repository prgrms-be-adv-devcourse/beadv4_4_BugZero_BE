package com.bugzero.rarego.boundedContext.payment.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.payment.domain.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByMemberId(Long memberId);
}
