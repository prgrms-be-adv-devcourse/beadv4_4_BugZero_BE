package com.bugzero.rarego.boundedContext.auction.out;

import java.util.Optional;

import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
	// auctionId를 기준으로 필드명을 검색
	Page<Bid> findAllByAuctionIdOrderByBidTimeDesc(Long auctionId, Pageable pageable);
	// 가장 최근 입찰 내역 1건 조회
	Optional<Bid> findTopByAuctionIdOrderByBidTimeDesc(Long auctionId);
}
