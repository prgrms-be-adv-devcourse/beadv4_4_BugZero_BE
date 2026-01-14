package com.bugzero.rarego.boundedContext.payment.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
    Optional<Deposit> findByMemberIdAndAuctionId(Long memberId, Long auctionId);

    List<Deposit> findAllByAuctionIdAndStatus(Long auctionId, DepositStatus status);
}
