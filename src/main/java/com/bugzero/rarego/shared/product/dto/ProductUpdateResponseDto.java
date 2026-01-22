package com.bugzero.rarego.shared.product.dto;

import lombok.Builder;

@Builder
public record ProductUpdateResponseDto (
	Long productId,
	Long auctionId
){
}
