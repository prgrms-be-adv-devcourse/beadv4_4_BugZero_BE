package com.bugzero.rarego.boundedContext.auction.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;

import jakarta.persistence.LockModeType;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
	// 비관적 락 처리
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Auction a where a.id = :id")
	Optional<Auction> findByIdWithLock(@Param("id") Long id);
}
