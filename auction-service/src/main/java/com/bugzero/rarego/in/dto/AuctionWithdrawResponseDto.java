package com.bugzero.rarego.in.dto;

import com.bugzero.rarego.shared.auction.type.AuctionStatus;

public record AuctionWithdrawResponseDto(
	Long auctionId,
	Long productId,
	AuctionStatus beforeStatus,
	AuctionStatus currentStatus,
	String message
) {
	public static AuctionWithdrawResponseDto of(Long auctionId, Long productId, AuctionStatus beforeStatus) {
		return new AuctionWithdrawResponseDto(
			auctionId,
			productId,
			beforeStatus,
			AuctionStatus.WITHDRAWN,
			"상품이 판매 포기 처리되었습니다."
		);
	}
}
