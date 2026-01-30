package com.bugzero.rarego.shared.auction.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;

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
		ProductAuctionResponseDto product,
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
			.actionRequired(resolveActionRequired(auction, order, bidCount))
			.build();
	}

	private static String resolveTitle(ProductAuctionResponseDto product) {
		return (product != null) ? product.getName() : "정보 없음";
	}

	private static String resolveThumbnail(ProductAuctionResponseDto product) {
		return (product != null) ? product.getThumbnailUrl() : null;
	}

	private static long resolvePrice(Auction auction, AuctionOrder order) {
		// 낙찰되어 주문이 생성된 경우 -> 최종 낙찰가(FinalPrice)
		if (auction.getStatus() == AuctionStatus.ENDED && order != null) {
			return order.getFinalPrice();
		}
		// 그 외(진행중, 유찰 등) -> 현재가(없으면 시작가)
		return auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartPrice();
	}

	private static boolean resolveActionRequired(Auction auction, AuctionOrder order, int bidCount) {
		// 경매가 종료(ENDED) 상태가 아니라면 조치 불필요
		if (auction.getStatus() != AuctionStatus.ENDED) {
			return false;
		}

		// 유찰: 입찰자가 단 한 명도 없는 경우
		boolean isNoBids = (bidCount == 0);

		// 미결제: 주문은 생성되었으나(낙찰), 결제 실패(FAILED) 상태인 경우
		boolean isPaymentFailed = (order != null && order.getStatus() == AuctionOrderStatus.FAILED);

		// 둘 중 하나라도 해당되면 재등록 등의 조치가 필요함
		return isNoBids || isPaymentFailed;
	}


}
