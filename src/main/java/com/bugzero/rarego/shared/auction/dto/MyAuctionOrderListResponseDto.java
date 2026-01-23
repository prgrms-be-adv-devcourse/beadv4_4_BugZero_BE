package com.bugzero.rarego.shared.auction.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;

public record MyAuctionOrderListResponseDto(
	Long orderId,
	Long auctionId,
	String productName,
	String thumbnailUrl,
	int finalPrice,
	AuctionOrderStatus orderStatus,
	String statusDescription,
	LocalDateTime tradeDate,
	boolean auctionRequired
) {
	public static MyAuctionOrderListResponseDto from (
		AuctionOrder order,
		Product product,
		String thumbnailUrl
	) {
		String description = convertStatusToDescription(order.getStatus());
		boolean isAuctionRequired = (order.getStatus() == AuctionOrderStatus.PROCESSING);

		return new MyAuctionOrderListResponseDto(
			order.getId(),
			order.getAuctionId(),
			// NPE 방지
			product != null ? product.getName() : "Unknown Product",
			thumbnailUrl,
			order.getFinalPrice(),
			order.getStatus(),
			description,
			order.getCreatedAt(),
			isAuctionRequired
		);
	}

	// 상태값을 설명으로 바꿔주는 메서드
	private static String convertStatusToDescription(AuctionOrderStatus status) {
		if (status == null) return "-";

		return switch (status) {
			case PROCESSING -> "결제 대기중";
			case SUCCESS -> "결제 완료";
			case FAILED -> "결제 실패/취소";
			default -> "기타";
		};
	}
}
