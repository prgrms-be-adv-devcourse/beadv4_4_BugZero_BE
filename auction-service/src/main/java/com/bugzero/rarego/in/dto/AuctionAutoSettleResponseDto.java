package com.bugzero.rarego.in.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.bugzero.rarego.domain.Auction;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;

@Builder
public record AuctionAutoSettleResponseDto(
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime requestTime,
	Integer processedCount,
	Integer successCount,
	Integer failCount,
	List<SettlementDetail> details
) {
	@Builder
	public record SettlementDetail(
		Long auctionId,
		String result,   // SUCCESS_BID, FAILED_NO_BIDS
		Long winnerId
	) {
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
	public static AuctionAutoSettleResponseDto from(
		LocalDateTime requestTime,
		List<Auction> auctions,
		int successCount,
		int failCount,
		List<SettlementDetail> details
	) {
		return AuctionAutoSettleResponseDto.builder()
			.requestTime(requestTime)
			.processedCount(auctions.size())
			.successCount(successCount)
			.failCount(failCount)
			.details(details)
			.build();
	}
}
