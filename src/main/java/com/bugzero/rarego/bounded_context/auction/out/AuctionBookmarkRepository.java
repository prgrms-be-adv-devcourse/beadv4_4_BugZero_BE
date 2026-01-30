package com.bugzero.rarego.bounded_context.auction.out;

import com.bugzero.rarego.bounded_context.auction.domain.AuctionBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionBookmarkRepository extends JpaRepository<AuctionBookmark, Long> {
    boolean existsByAuctionIdAndMemberId(Long auctionId, Long memberId);

    Page<AuctionBookmark> findAllByMemberId(Long memberId, Pageable pageable);
}