package com.bugzero.rarego.in.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionHeartbeatEventDto(
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime serverTime
) {
	public String getType() {
		return "PING";
	}

	public static AuctionHeartbeatEventDto create() {
		return new AuctionHeartbeatEventDto(
			LocalDateTime.now()
		);
	}
}
