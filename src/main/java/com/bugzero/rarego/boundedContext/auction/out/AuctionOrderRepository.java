package com.bugzero.rarego.boundedContext.auction.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;

public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, Long> {
    Optional<AuctionOrder> findByAuctionId(Long auctionId);
}
