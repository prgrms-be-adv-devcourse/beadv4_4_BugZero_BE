package com.bugzero.rarego.shared.auction.dto;

import com.bugzero.rarego.bounded_context.auction.domain.AuctionOrder;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionOrderStatus;
import java.time.LocalDateTime;

public record AuctionOrderResponseDto(
	Long orderId,
	Long auctionId,
	// BUYER, SELLER
	String viewerRole,
	AuctionOrderStatus orderStatus,
	String statusDescription,
	LocalDateTime createdAt,
	ProductInfo productInfo,
	PaymentInfo paymentInfo,
	TraderInfo trader,
	ShippingInfo shippingInfo
) {
	public record ProductInfo(String title, String thumbnailUrl) {}
	public record PaymentInfo(int finalPrice, int depositUsed, int paymentAmount) {}
	public record TraderInfo(String nickname, String contact) {}
	public record ShippingInfo(String receiverName, String address, String trackingNumber) {}

	public static AuctionOrderResponseDto from(
		AuctionOrder order,
		String viewerRole,
		String statusDescription,
		String productTitle,
		String productThumbnail,
		String traderNickname,
		String traderContact
	) {

		int finalPrice = order.getFinalPrice();
		int depositUsed = (int) (finalPrice * 0.1);
		int paymentAmount = finalPrice - depositUsed;

		return new AuctionOrderResponseDto(
			order.getId(),
			order.getAuctionId(),
			viewerRole,
			order.getStatus(),
			statusDescription,
			order.getCreatedAt(),
			new ProductInfo(productTitle, productThumbnail),
			new PaymentInfo(
				finalPrice,
				depositUsed,
				paymentAmount
			),
			new TraderInfo(traderNickname, traderContact),
			new ShippingInfo(null, null, null)
		);
	}
}