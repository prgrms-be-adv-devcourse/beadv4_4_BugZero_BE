package com.bugzero.rarego.boundedContext.product.auction.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionCreateAuctionUseCase {
	private final AuctionRepository auctionRepository;

	@Transactional
	// 신규상품 경매 정보 생성
	public long createAuction(Long productId,String sellerUUID, ProductAuctionRequestDto productAuctionRequestDto) {
		//TODO sellerUUID를 통해 memberId를 반환하는 메서드 추가 (support에 생성)
		Long sellerId = 1L;

		int tickSize = determineTickSize(productAuctionRequestDto.startPrice());

		return auctionRepository
			.save(productAuctionRequestDto.toEntity(productId,sellerId, tickSize))
			.getId();
	}

	// 시작가에 따라 호가단위 결정
	private int determineTickSize(int startPrice) {
		if (startPrice < 10000) {
			return 500;
		} else if (startPrice < 50000) {
			return 1000;
		} else if (startPrice < 100000) {
			return 2000;
		} else if (startPrice < 300000) {
			return 5000;
		} else if (startPrice < 1000000) {
			return 10000;
		} else {
			return 30000; // 100만 원 이상
		}
	}
}
