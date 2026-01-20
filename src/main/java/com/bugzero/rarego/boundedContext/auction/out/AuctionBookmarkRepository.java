package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuctionBookmarkRepository extends JpaRepository<AuctionBookmark, Long> {
    boolean existsByAuctionIdAndMemberId(Long auctionId, Long memberId);

    Optional<AuctionBookmark> findByAuctionIdAndMemberId(Long auctionId, Long memberId);

    Page<AuctionBookmark> findAllByMemberId(Long memberId, Pageable pageable);
}