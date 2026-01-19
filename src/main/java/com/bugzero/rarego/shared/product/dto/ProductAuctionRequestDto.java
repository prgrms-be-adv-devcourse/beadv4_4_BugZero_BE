package com.bugzero.rarego.shared.product.dto;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ProductAuctionRequestDto(
	@Min(value = 100, message = "입찰 시작가는 최소100원 이상입니다.")
	int startPrice,
	@NotNull
	@Min(value = 1, message = "경매기간은 최소 1일이상부터 가능합니다.")
	@Max(value = 30, message = "경매기간은 최대 30일까지만 가능합니다.")
	Integer durationDays
) {
	public Auction toEntity(Long productId, Long sellerId, int tickSize) {
		return Auction.builder()
			.productId(productId)
			.sellerId(sellerId)
			.startPrice(startPrice)
			.tickSize(tickSize)
			.durationDays(durationDays)
			.build();
	}
}
