package com.bugzero.rarego.shared.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuctionRelistRequestDto {
	private Long startPrice;
	private Long tickSize;
	private int durationDays;
}
