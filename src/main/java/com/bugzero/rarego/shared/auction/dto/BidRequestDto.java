package com.bugzero.rarego.shared.auction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BidRequestDto(
        @NotNull(message = "입찰 가격은 필수입니다.")
        @Positive(message = "입찰 가격은 양수여야 합니다.")
        Long bidAmount
) {
}
