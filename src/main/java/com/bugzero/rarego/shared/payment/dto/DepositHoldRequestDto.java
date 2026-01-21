package com.bugzero.rarego.shared.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepositHoldRequestDto(
        @NotNull Integer amount,
		@NotBlank String memberPublicId,
        @NotNull Long auctionId) {
}
