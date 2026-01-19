package com.bugzero.rarego.boundedContext.payment.in.dto;

import java.time.LocalDateTime;

public record RefundResponse(
        Long transactionId,
        Long auctionId,
        int refundAmount,
        String buyerId,
        int currentBalance,
        String status,
        LocalDateTime createdAt) {
}
