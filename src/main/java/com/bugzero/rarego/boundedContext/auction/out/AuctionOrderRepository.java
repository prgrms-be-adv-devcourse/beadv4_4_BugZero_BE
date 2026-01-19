package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, Long> {

    Optional<AuctionOrder> findByAuctionId(Long auctionId);

    // 경매 ID 목록으로 주문 정보 조회
    List<AuctionOrder> findAllByAuctionIdIn(Collection<Long> auctionIds);

    List<AuctionOrder> findByStatusAndCreatedAtBefore(AuctionOrderStatus status, LocalDateTime cutoffTime);
}
