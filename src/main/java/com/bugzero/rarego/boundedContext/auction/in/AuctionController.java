package com.bugzero.rarego.boundedContext.auction.in;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Tag(name = "Auction", description = "경매 관련 API")
public class AuctionController {

	private final AuctionFacade auctionFacade;

	@Operation(summary = "경매 입찰", description = "경매에 입찰합니다")
	@PostMapping("/{auctionId}/bids")
	public SuccessResponseDto<BidResponseDto> createBid(
		@PathVariable Long auctionId,
		@Valid @RequestBody BidRequestDto bidRequestDto,
		@AuthenticationPrincipal MemberPrincipal memberPrincipal
	) {
		// JWT 토큰 생성 시 저장한 memberPublicId를 이용해 조회
		String memberPublicId = memberPrincipal.publicId();

		// TODO: 임의

		SuccessResponseDto<BidResponseDto> response = auctionFacade.createBid(
			auctionId,
			memberPublicId,
			bidRequestDto.bidAmount().intValue()
		);

		return response;
	}

	// 경매 입찰 기록 조회 (GET /api/v1/auctions/{auctionId}/bids)
	@GetMapping("/{auctionId}/bids")
	public PagedResponseDto<BidLogResponseDto> getBids(
		@PathVariable Long auctionId,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return auctionFacade.getBidLogs(auctionId, pageable);
	}
}
