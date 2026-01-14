package com.bugzero.rarego.shared.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProductAuctionRequestDto(
	@Min(value = 100, message = "입찰 시작가는 최소100원 이상입니다.")
	int startPrice,
	@Min(value = 1, message = "걍매기간은 최소 1일이상부터 가능합니다.")
	@Max(value = 30, message = "경매기간은 최대 30일까지만 가능합니다.")
	int durationDays
) {
}
