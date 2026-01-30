package com.bugzero.rarego.bounded_context.product.auction.in;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.bounded_context.product.auction.app.AuctionDetermineStartAuctionUseCase;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionStartAuctionController {

	private final AuctionDetermineStartAuctionUseCase auctionDetermineStartAuctionUseCase;

	//TODO AUTH를 통해 인증인가 구현
	@PatchMapping("/{auctionId}/startTime")
	public SuccessResponseDto<Long> deterMineStartAuction(
		@PathVariable Long auctionId
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			auctionDetermineStartAuctionUseCase.determineStartAuction(auctionId));
	}
}
