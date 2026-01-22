package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, Long> {

    Optional<AuctionOrder> findByAuctionId(Long auctionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM AuctionOrder o WHERE o.auctionId = :auctionId")
    Optional<AuctionOrder> findByAuctionIdForUpdate(@Param("auctionId") Long auctionId);

    // 경매 ID 목록으로 주문 정보 조회
    List<AuctionOrder> findAllByAuctionIdIn(Collection<Long> auctionIds);

    // 타임아웃 대상 주문 조회 (PROCESSING 상태 + 생성일 기준)
    Slice<AuctionOrder> findByStatusAndCreatedAtBefore(AuctionOrderStatus status, LocalDateTime deadline,
                                                       Pageable pageable);

    // status가 NULL이면 무시(=전체 조회), 값이 있으면 일치하는 것만 조회
    @Query("SELECT ao FROM AuctionOrder ao WHERE ao.bidderId = :bidderId AND (:status IS NULL OR ao.status = :status)")
    Page<AuctionOrder> findAllByBidderIdAndStatus(
            @Param("bidderId") Long bidderId,
            @Param("status") AuctionOrderStatus status,
            Pageable pageable
    );

    @Query("""
                SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
                FROM AuctionOrder o
                JOIN AuctionMember m ON o.bidderId = m.id
                WHERE m.publicId = :publicId
                AND o.status = :status
            """)
    boolean existsByBuyerPublicIdAndStatus(
            @Param("publicId") String publicId,
            @Param("status") AuctionOrderStatus status
    );
}
