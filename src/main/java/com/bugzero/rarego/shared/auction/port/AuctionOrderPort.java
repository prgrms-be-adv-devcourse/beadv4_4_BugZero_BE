package com.bugzero.rarego.shared.auction.port;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

public interface AuctionOrderPort {
    Optional<AuctionOrderDto> findByAuctionId(Long auctionId);

    // 동시성 제어를 위한 비관적 락 조회
    Optional<AuctionOrderDto> findByAuctionIdForUpdate(Long auctionId);

    void completeOrder(Long auctionId);

    void failOrder(Long auctionId);

    AuctionOrderDto refundOrderWithLock(Long auctionId);

    Slice<AuctionOrderDto> findTimeoutOrders(LocalDateTime deadline, Pageable pageable);
}
