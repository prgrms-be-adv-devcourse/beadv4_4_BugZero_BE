package com.bugzero.rarego.boundedContext.product.auction.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionCreateAuctionUseCase {
	private final AuctionRepository auctionRepository;
	private final ProductAuctionSupport productAuctionSupport;

	@Transactional
	// 신규상품 경매 정보 생성
	public long createAuction(Long productId,String publicId, ProductAuctionRequestDto productAuctionRequestDto) {
		AuctionMember seller = productAuctionSupport.getAuctionMember(publicId);

		int tickSize = productAuctionSupport.determineTickSize(productAuctionRequestDto.startPrice());

		return auctionRepository
			.save(productAuctionRequestDto.toEntity(productId, seller.getId()))
			.getId();
	}


}
