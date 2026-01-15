package com.bugzero.rarego.boundedContext.auction.in;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

	private final AuctionFacade auctionFacade;

	// 경매 입찰 (POST /api/v1/auctions/{auctionId}/bids)
	@PostMapping("/{auctionId}/bids")
	public SuccessResponseDto<BidResponseDto> createBid(
		@PathVariable Long auctionId,
		@Valid @RequestBody BidRequestDto bidRequestDto,
		@AuthenticationPrincipal UserDetails userDetails
	) {
		// TODO: 실제 SecurityContext에서 ID 추출
		// Long memberId = Long.valueOf(userDetails.getUsername());
		Long memberId = 2L; // 테스트용 임시 ID

		SuccessResponseDto<BidResponseDto> response = auctionFacade.createBid(
			auctionId,
			memberId,
			bidRequestDto.bidAmount().intValue()
		);

		return response;
	}
}
