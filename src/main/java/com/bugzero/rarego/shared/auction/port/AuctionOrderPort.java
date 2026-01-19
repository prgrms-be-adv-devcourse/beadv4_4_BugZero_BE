package com.bugzero.rarego.shared.auction.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

public interface AuctionOrderPort {
    Optional<AuctionOrderDto> findByAuctionId(Long auctionId);

    void completeOrder(Long auctionId);

    void failOrder(Long auctionId);

    List<AuctionOrderDto> findTimeoutOrders(LocalDateTime deadline);
}
