package com.bugzero.rarego.boundedContext.product.auction.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionUpdateAuctionUseCase {
	private final ProductAuctionSupport productAuctionSupport;

	@Transactional
	public Long updateAuction(String publicId, ProductAuctionUpdateDto dto) {

		//sellerId를 가져오기 위한 AuctionMember 가져오는 메서드
		AuctionMember seller = productAuctionSupport.getAuctionMember(publicId);
		//상품 수정 시 내부 api 호출을 통해 경매정보를 수정 즉 이미 상품이 있는 것으로 상정.
		Auction auction = productAuctionSupport.getAuction(dto.auctionId());
		//경매정보를 수정할 수 있는지 확인
		productAuctionSupport.isAbleToChange(seller, auction);
		//경매 정보 수정 (시작가격 수정 시 그에 따라 호가단위도 바뀔 수 있기 때문에 다시 설정)
		int tickSize = productAuctionSupport.determineTickSize(dto.startPrice());

		auction.update(dto.durationDays(), dto.startPrice(), tickSize);
		//경매 id 값 반환
		return auction.getId();
	}
}
