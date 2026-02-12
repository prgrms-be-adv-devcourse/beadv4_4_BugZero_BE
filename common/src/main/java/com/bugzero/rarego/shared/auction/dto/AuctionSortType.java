package com.bugzero.rarego.shared.auction.dto;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuctionSortType {
	CLOSING_SOON("마감임박순", Sort.by(Sort.Direction.ASC, "endTime")),
	NEWEST("최신순", Sort.by(Sort.Direction.DESC, "createdAt"));

	private final String description;
	private final Sort sort;
}
