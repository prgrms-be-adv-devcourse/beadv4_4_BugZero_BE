package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    // 비관적 락 처리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :id")
    Optional<Auction> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT a FROM Auction a
                WHERE a.status = 'IN_PROGRESS'
                AND a.endTime <= :now
            """)
    List<Auction> findExpiredInProgressAuctionsWithLock(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
