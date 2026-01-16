package com.bugzero.rarego.boundedContext.payment.in.dto;

import java.time.LocalDateTime;

public record AuctionFinalPaymentResponseDto(
        Long paymentId,
        Long auctionId,
        String buyerId,
        int finalPrice,
        int depositAmount,
        int paidAmount,
        int balanceAfter,
        String status,
        LocalDateTime createdAt) {
    public static AuctionFinalPaymentResponseDto of(
            Long paymentId,
            Long auctionId,
            String buyerPublicId,
            int finalPrice,
            int depositAmount,
            int balanceAfter,
            LocalDateTime createdAt) {
        return new AuctionFinalPaymentResponseDto(
                paymentId,
                auctionId,
                buyerPublicId,
                finalPrice,
                depositAmount,
                finalPrice - depositAmount,
                balanceAfter,
                "PAID",
                createdAt);
    }
}
