package com.bugzero.rarego.boundedContext.payment.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.member.id = :memberId")
    Optional<Wallet> findByMemberIdForUpdate(@Param("memberId") Long memberId);
}
