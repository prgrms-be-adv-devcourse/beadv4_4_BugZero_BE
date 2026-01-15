package com.bugzero.rarego.boundedContext.auction.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionFacade {

	private final AuctionCreateBidUseCase auctionCreateBidUseCase;

	@Transactional
	public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, Long memberId, int bidAmount) {
		return auctionCreateBidUseCase.createBid(auctionId, memberId, bidAmount);
	}

}
