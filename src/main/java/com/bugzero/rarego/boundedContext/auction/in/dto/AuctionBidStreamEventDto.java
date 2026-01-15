package com.bugzero.rarego.boundedContext.auction.in.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * SSE 스트림으로 전송되는 입찰 이벤트 데이터
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionBidStreamEventDto(
        String type,          // "CONNECT", "BID", "AUCTION_ENDED", "PING"
        Long auctionId,
        Integer currentPrice, // 현재가 (CONNECT 시)
        Integer bidAmount,    // 입찰가 (BID 시)
        String bidderName,    // 마스킹된 입찰자명 (BID 시)
        String winnerName,    // 마스킹된 낙찰자명 (AUCTION_ENDED 시)
        Integer finalPrice,   // 최종 낙찰가 (AUCTION_ENDED 시)

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime bidTime,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime serverTime,

        String message        // 연결 메시지 등
) {
    // Factory Methods
    public static AuctionBidStreamEventDto connect(Long auctionId, Integer currentPrice) {
        return new AuctionBidStreamEventDto(
                "CONNECT",
                auctionId,
                currentPrice,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                "연결되었습니다."
        );
    }

    public static AuctionBidStreamEventDto bid(
            Long auctionId,
            Integer bidAmount,
            String bidderName,
            LocalDateTime bidTime
    ) {
        return new AuctionBidStreamEventDto(
                "BID",
                auctionId,
                null,
                bidAmount,
                maskName(bidderName),
                null,
                null,
                bidTime,
                LocalDateTime.now(),
                null
        );
    }

    public static AuctionBidStreamEventDto auctionEnded(
            Long auctionId,
            Integer finalPrice,
            String winnerName
    ) {
        return new AuctionBidStreamEventDto(
                "AUCTION_ENDED",
                auctionId,
                null,
                null,
                null,
                winnerName != null ? maskName(winnerName) : null,
                finalPrice,
                null,
                LocalDateTime.now(),
                null
        );
    }

    public static AuctionBidStreamEventDto ping() {
        return new AuctionBidStreamEventDto(
                "PING",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                "heartbeat"
        );
    }

    /**
     * 개인정보 마스킹 - 가운데 글자를 * 처리
     * 예: "김철수" -> "김*수", "legoKing" -> "legoK**"
     */
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