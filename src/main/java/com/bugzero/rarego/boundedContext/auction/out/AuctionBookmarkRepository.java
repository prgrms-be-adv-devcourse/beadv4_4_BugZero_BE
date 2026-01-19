package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionBookmarkRepository extends JpaRepository<AuctionBookmark, Long> {
    boolean existsByAuctionIdAndMemberId(Long auctionId, Long memberId);
}