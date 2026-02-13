package com.bugzero.rarego.in.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

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
}
