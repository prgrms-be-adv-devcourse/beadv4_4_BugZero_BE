package com.bugzero.rarego.boundedContext.auction.in.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionBidEventDto(
        Long auctionId,
        Integer bidAmount,
        String bidderName,
        LocalDateTime bidTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime serverTime
) {
    public String getType() {
        return "BID";
    }

    public String getMaskedBidderName() {
        return maskName(bidderName);
    }

    public static AuctionBidEventDto create(
            Long auctionId,
            Integer bidAmount,
            String bidderName,
            LocalDateTime bidTime
    ) {
        return new AuctionBidEventDto(
                auctionId,
                bidAmount,
                bidderName,
                bidTime,
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
