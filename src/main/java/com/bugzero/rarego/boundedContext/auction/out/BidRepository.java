package com.bugzero.rarego.boundedContext.auction.out;

import java.util.Optional;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface BidRepository extends JpaRepository<Bid, Long> {

	@Query("""
                SELECT b FROM Bid b
                WHERE b.auction.id = :auctionId
                ORDER BY b.bidAmount DESC, b.bidTime ASC
            """)
	Optional<Bid> findTopByAuctionId(@Param("auctionId") Long auctionId);

	boolean existsByAuctionId(Long auctionId);

	// 가장 높은 입찰 1개만 (같은 금액이면 먼저 입찰한 것)
	Optional<Bid> findTopByAuctionIdOrderByBidAmountDescBidTimeAsc(Long auctionId);

	// 가장 최근 입찰 내역 1건 조회
	Optional<Bid> findTopByAuctionIdOrderByBidTimeDesc(Long auctionId);

	// auctionId를 기준으로 필드명을 검색
	// publicId 조회를 위해 bidder fetch join 필요
	@EntityGraph(attributePaths = {"bidder"})
	@Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.bidTime DESC")
	Page<Bid> findAllByAuctionIdOrderByBidTimeDesc(@Param("auctionId") Long auctionId, Pageable pageable);

	// 내가 입찰한 기록 중, 각 경매별로 '가장 마지막에 입찰한(MAX(id))' 건만 추출
	// @EntityGraph: auction 연관관계를 Eager로 가져와 N+1 문제 해결 (JPQL의 FETCH JOIN 대체)
	// 계획에서는 JOIN FETCH로 구현 예정이었으나 메모리 페이징 경고가 생길 수 있다고 하여 변경
	@EntityGraph(attributePaths = {"auction"})
	@Query(value = """

		SELECT b 
        FROM Bid b 
        WHERE b.bidder.id = :memberId 
          AND (:status IS NULL OR b.auction.status = :status)
          AND b.id IN (
              SELECT MAX(b2.id) 
              FROM Bid b2 
              WHERE b2.bidder.id = :memberId 
              GROUP BY b2.auction.id
          )
        ORDER BY b.bidTime DESC
        """,
		countQuery = """
        SELECT COUNT(b) 
        FROM Bid b 
        WHERE b.bidder.id = :memberId 
          AND (:status IS NULL OR b.auction.status = :status)
          AND b.id IN (
              SELECT MAX(b2.id) 
              FROM Bid b2 
              WHERE b2.bidder.id = :memberId 
              GROUP BY b2.auction.id
          )
        """)
	Page<Bid> findMyBids(@Param("memberId") Long memberId,
		@Param("status") AuctionStatus status,
		Pageable pageable);
}
