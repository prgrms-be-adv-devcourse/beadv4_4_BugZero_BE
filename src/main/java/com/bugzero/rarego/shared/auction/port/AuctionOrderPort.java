package com.bugzero.rarego.shared.auction.port;

import java.util.Optional;

import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

public interface AuctionOrderPort {
    Optional<AuctionOrderDto> findByAuctionId(Long auctionId);

    void completeOrder(Long auctionId);
}
