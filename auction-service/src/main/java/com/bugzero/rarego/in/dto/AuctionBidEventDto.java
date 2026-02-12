package com.bugzero.rarego.in.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

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
}
