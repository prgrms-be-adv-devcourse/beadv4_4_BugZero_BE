package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    // auctionId를 기준으로 필드명을 검색
    Page<Bid> findAllByAuctionIdOrderByBidTimeDesc(Long auctionId, Pageable pageable);

    @Query("""
                SELECT b FROM Bid b
                WHERE b.auctionId = :auctionId
                ORDER BY b.bidAmount DESC, b.bidTime ASC
            """)
    Optional<Bid> findTopByAuctionId(@Param("auctionId") Long auctionId);

    boolean existsByAuctionId(Long auctionId);

    // 가장 높은 입찰 1개만 (같은 금액이면 먼저 입찰한 것)
    Optional<Bid> findTopByAuctionIdOrderByBidAmountDescBidTimeAsc(Long auctionId);

    // 가장 최근 입찰 내역 1건 조회
    Optional<Bid> findTopByAuctionIdOrderByBidTimeDesc(Long auctionId);
}
