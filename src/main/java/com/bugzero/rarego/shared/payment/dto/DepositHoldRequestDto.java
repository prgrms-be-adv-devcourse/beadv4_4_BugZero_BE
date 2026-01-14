package com.bugzero.rarego.shared.payment.dto;

import jakarta.validation.constraints.NotNull;

public record DepositHoldRequestDto(
        @NotNull Integer amount,
        @NotNull Long memberId,
        @NotNull Long auctionId) {
}
