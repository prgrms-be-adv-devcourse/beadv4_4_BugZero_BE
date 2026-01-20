package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

	private final AuctionFacade auctionFacade;

	// 경매 상태/현재가 Bulk 조회
	@Operation(summary = "경매 목록 조회", description = "검색 조건(키워드, 카테고리, 상태)과 정렬 조건에 따라 경매 목록을 조회합니다.")
	@GetMapping
	public PagedResponseDto<AuctionListResponseDto> getAuctions(
		@ModelAttribute AuctionSearchCondition condition,
		@PageableDefault(size = 10) Pageable pageable
	) {
		return auctionFacade.getAuctions(condition, pageable);
	}

	@GetMapping("/{auctionId}")
	public SuccessResponseDto<AuctionDetailResponseDto> getAuctionDetail(
		@PathVariable Long auctionId,
		@AuthenticationPrincipal MemberPrincipal principal
	) {
		String memberPublicId = (principal != null) ? principal.publicId() : null;
		return auctionFacade.getAuctionDetail(auctionId, memberPublicId);
	}

	@PostMapping("/{auctionId}/bids")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponseDto<BidResponseDto> createBid(
		@PathVariable Long auctionId,
		@Valid @RequestBody BidRequestDto bidRequestDto,
		@AuthenticationPrincipal MemberPrincipal principal
	) {
		SuccessResponseDto<BidResponseDto> response = auctionFacade.createBid(
			auctionId,
			principal.publicId(),
			bidRequestDto.bidAmount().intValue()
		);
		return response;
	}

	@GetMapping("/{auctionId}/bids")
	public PagedResponseDto<BidLogResponseDto> getBids(
		@PathVariable Long auctionId,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return auctionFacade.getBidLogs(auctionId, pageable);
	}

	@GetMapping("/{auctionId}/order")
	public SuccessResponseDto<AuctionOrderResponseDto> getAuctionOrder(
		@PathVariable Long auctionId,
		@AuthenticationPrincipal MemberPrincipal principal
	) {
		return auctionFacade.getAuctionOrder(auctionId, principal.publicId());
	}
}