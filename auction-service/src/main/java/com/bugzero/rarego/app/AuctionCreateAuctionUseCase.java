package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.domain.AuctionMember;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuctionCreateAuctionUseCase {
	private final AuctionRepository auctionRepository;
	private final AuctionSupport auctionSupport;

    // 신규상품 경매 정보 생성
    @Transactional
    public long createAuction(Long productId, String publicId, ProductAuctionRequestDto dto) {
        AuctionMember seller = auctionSupport.getPublicMember(publicId);

		Auction auction = Auction.builder()
			.productId(productId)
			.sellerId(seller.getId())
			.startPrice(dto.startPrice())
			.durationDays(dto.durationDays())
			.startTime(null)
			.endTime(null)
			.build();

		return auctionRepository.save(auction).getId();
	}
}
