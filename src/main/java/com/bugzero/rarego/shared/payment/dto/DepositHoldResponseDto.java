package com.bugzero.rarego.shared.payment.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;

public record DepositHoldResponse(
        Long depositId,
        Long auctionId,
        int amount,
        String status,
        LocalDateTime createdAt) {
    public static DepositHoldResponse from(Deposit deposit) {
        return new DepositHoldResponse(
                deposit.getId(),
                deposit.getAuctionId(),
                deposit.getAmount(),
                deposit.getStatus().name(),
                deposit.getCreatedAt());
    }
}
