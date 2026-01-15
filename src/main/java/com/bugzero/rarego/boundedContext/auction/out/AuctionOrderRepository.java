package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, Long> {

    Optional<AuctionOrder> findByAuctionId(Long auctionId);

    boolean existsByAuctionId(Long auctionId);
}
