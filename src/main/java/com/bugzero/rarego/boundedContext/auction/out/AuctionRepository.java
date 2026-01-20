package com.bugzero.rarego.boundedContext.auction.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;

import jakarta.persistence.LockModeType;

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

    //삭제가 되지 않은 경매 정보만 반환
    Optional<Auction> findByIdAndDeletedIsFalse(Long auctionId);

    // 필터링 없는 조건
    Page<Auction> findAllByProductIdIn(Collection<Long> productIds, Pageable pageable);

    // 상태 필터링이 있을 때
    Page<Auction> findAllByProductIdInAndStatusIn(
        Collection<Long> productIds,
        Collection<AuctionStatus> statuses,
        Pageable pageable
    );
}