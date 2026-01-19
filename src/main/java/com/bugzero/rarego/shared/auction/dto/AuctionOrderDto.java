package com.bugzero.rarego.shared.auction.dto;

import java.time.LocalDateTime;

public record AuctionOrderDto(
                Long orderId,
                Long auctionId,
                Long sellerId,
                Long bidderId,
                int finalPrice,
                String status,
                LocalDateTime createdAt) {
}
