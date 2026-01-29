package com.bugzero.rarego.boundedContext.auction.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionDeleteAuctionUseCase {
	private final AuctionSupport auctionSupport;

	@Transactional
	public void deleteAuction(String publicId, Long productId) {
		AuctionMember seller = auctionSupport.getPublicMember(publicId);
		//productId는 내부에서 호출된 api를 통해 가져왔기 때문에 신뢰할 수 있음.따라서 따로 실제 상품이 있는 검증X
		Auction auction = auctionSupport.getAuctionByProductId(productId);
		//상품을 삭제할 수 있는지 확인
		auctionSupport.isAbleToChange(seller, auction);

		auction.softDelete();
	}
}
