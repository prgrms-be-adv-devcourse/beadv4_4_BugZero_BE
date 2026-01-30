package com.bugzero.rarego.bounded_context.payment.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.bounded_context.payment.domain.Wallet;
import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w JOIN FETCH w.member WHERE w.member.id = :memberId")
    Optional<Wallet> findByMemberIdForUpdate(@Param("memberId") Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT w FROM Wallet w WHERE w.member.id IN :memberIds")
	List<Wallet> findAllByMemberIdInForUpdate(@Param("memberIds") List<Long> memberIds);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.member.id = :memberId")
	int increaseBalance(Long memberId, int amount);

	Optional<Wallet> findByMemberId(Long memberId);
}
