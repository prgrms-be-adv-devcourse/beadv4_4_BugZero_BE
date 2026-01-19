package com.bugzero.rarego.boundedContext.auction.in.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionConnectEventDto(
        Long auctionId,
        Integer currentPrice,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime serverTime,
        String message
) {
    public String getType() {
        return "CONNECT";
    }

    public static AuctionConnectEventDto create(Long auctionId, Integer currentPrice) {
        return new AuctionConnectEventDto(
                auctionId,
                currentPrice,
                LocalDateTime.now(),
                "연결되었습니다."
        );
    }
}