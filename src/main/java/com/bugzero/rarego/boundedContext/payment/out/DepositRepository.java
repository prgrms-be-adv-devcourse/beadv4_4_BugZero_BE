package com.bugzero.rarego.boundedContext.payment.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
    Optional<Deposit> findByMemberIdAndAuctionId(Long memberId, Long auctionId);

    @Query("SELECT d FROM Deposit d JOIN FETCH d.member WHERE d.auctionId = :auctionId AND d.status = :status")
    List<Deposit> findAllByAuctionIdAndStatusWithMember(
            @Param("auctionId") Long auctionId,
            @Param("status") DepositStatus status);

    @Query("SELECT d FROM Deposit d JOIN FETCH d.member WHERE d.auctionId = :auctionId AND d.status = :status AND d.member.id != :memberId")
    List<Deposit> findAllByAuctionIdAndStatusAndMemberIdNotWithMember(
            @Param("auctionId") Long auctionId,
            @Param("status") DepositStatus status,
            @Param("memberId") Long memberId);
}
