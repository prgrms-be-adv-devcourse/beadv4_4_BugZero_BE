package com.bugzero.rarego.boundedContext.auction.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AuctionAutoResponseDto {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime requestTime;

    private Integer processedCount;
    private Integer successCount;
    private Integer failCount;
    private List<SettlementDetail> details;

    @Getter
    @Builder
    public static class SettlementDetail {
        private Long auctionId;
        private String result;   // SUCCESS_BID, FAILED_NO_BIDS
        private Long winnerId;

        public static SettlementDetail success(Long auctionId, Long winnerId) {
            return SettlementDetail.builder()
                    .auctionId(auctionId)
                    .result("SUCCESS_BID")
                    .winnerId(winnerId)
                    .build();
        }

        public static SettlementDetail failed(Long auctionId) {
            return SettlementDetail.builder()
                    .auctionId(auctionId)
                    .result("FAILED_NO_BIDS")
                    .winnerId(null)
                    .build();
        }
    }

    // 정적 팩토리 메서드
    public static AuctionAutoResponseDto from(
            LocalDateTime requestTime,
            List<Auction> auctions,
            int successCount,
            int failCount,
            List<SettlementDetail> details
    ) {
        return AuctionAutoResponseDto.builder()
                .requestTime(requestTime)
                .processedCount(auctions.size())
                .successCount(successCount)
                .failCount(failCount)
                .details(details)
                .build();
    }
}
