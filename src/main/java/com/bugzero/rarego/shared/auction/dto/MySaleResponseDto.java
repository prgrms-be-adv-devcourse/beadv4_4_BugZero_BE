package com.bugzero.rarego.shared.auction.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;

import lombok.Builder;

@Builder
public record MySaleResponseDto (
	Long auctionId,
	String title,
	String thumbnailUrl,
	long currentPrice,
	// 입찰 횟수
	int bidCount,
	AuctionStatus auctionStatus,
	// 주문 상태
	AuctionOrderStatus tradeStatus,
	LocalDateTime endTime,
	// 재등록 버튼 활성화 여부
	boolean actionRequired
) {
	public static MySaleResponseDto from(
		Auction auction,
		Product product,
		AuctionOrder order,
		int bidCount
	) {
		return MySaleResponseDto.builder()
			.auctionId(auction.getId())
			.title(resolveTitle(product))
			.thumbnailUrl(resolveThumbnail(product))
			.currentPrice(resolvePrice(auction, order))
			.bidCount(bidCount)
			.auctionStatus(auction.getStatus())
			.tradeStatus(order != null ? order.getStatus() : null)
			.endTime(auction.getEndTime())
			.actionRequired(resolveActionRequired(auction.getStatus()))
			.build();
	}

	private static String resolveTitle(Product product) {
		return (product != null) ? product.getName() : "정보 없음";
	}

	private static String resolveThumbnail(Product product) {
		if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
			return product.getImages().get(0).getImageUrl();
		}
		return null; // 또는 기본 이미지 URL
	}

	private static long resolvePrice(Auction auction, AuctionOrder order) {
		// 낙찰되어 주문이 생성된 경우 -> 최종 낙찰가(FinalPrice)
		if (auction.getStatus() == AuctionStatus.ENDED && order != null) {
			return order.getFinalPrice();
		}
		// 그 외(진행중, 유찰 등) -> 현재가(없으면 시작가)
		return auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartPrice();
	}

	private static boolean resolveActionRequired(AuctionStatus status) {
		// 유찰(ENDED)된 경우 재등록 필요
		return status == AuctionStatus.ENDED;
	}
}
