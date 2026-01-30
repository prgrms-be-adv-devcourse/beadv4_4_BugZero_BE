package com.bugzero.rarego.shared.payment.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.bounded_context.payment.domain.Deposit;

public record DepositHoldResponseDto(
        Long depositId,
        Long auctionId,
        int amount,
        String status,
        LocalDateTime createdAt) {
    public static DepositHoldResponseDto from(Deposit deposit) {
        return new DepositHoldResponseDto(
                deposit.getId(),
                deposit.getAuctionId(),
                deposit.getAmount(),
                deposit.getStatus().name(),
                deposit.getCreatedAt());
    }
}
