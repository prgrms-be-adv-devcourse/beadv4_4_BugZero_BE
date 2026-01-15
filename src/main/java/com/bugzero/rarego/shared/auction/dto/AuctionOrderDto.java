package com.bugzero.rarego.shared.auction.dto;

public record AuctionOrderDto(
                Long orderId,
                Long auctionId,
                Long bidderId,
                int finalPrice,
                String status) {
}
