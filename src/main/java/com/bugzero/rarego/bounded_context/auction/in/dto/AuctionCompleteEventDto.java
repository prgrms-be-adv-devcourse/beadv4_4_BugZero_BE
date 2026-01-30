package com.bugzero.rarego.bounded_context.auction.in.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionCompleteEventDto(
        Long auctionId,
        Integer finalPrice,
        String winnerName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime serverTime
) {
    public String getType() {
        return "AUCTION_ENDED";
    }

    public String getMaskedWinnerName() {
        return maskName(winnerName);
    }

    public static AuctionCompleteEventDto create(
            Long auctionId,
            Integer finalPrice,
            String winnerName
    ) {
        return new AuctionCompleteEventDto(
                auctionId,
                finalPrice,
                winnerName,
                LocalDateTime.now()
        );
    }

    private static String maskName(String name) {
        if (name == null || name.length() <= 2) {
            return name;
        }

        int length = name.length();
        int maskStart = length / 3;
        int maskEnd = length - length / 3;

        StringBuilder masked = new StringBuilder(name);
        for (int i = maskStart; i < maskEnd; i++) {
            masked.setCharAt(i, '*');
        }
        return masked.toString();
    }
}