package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class AuctionMemberController {

	private final AuctionFacade auctionFacade;

	@GetMapping("/me/bids")
	public PagedResponseDto<MyBidResponseDto> getMyBids(
		@RequestParam(required = false) AuctionStatus auctionStatus,
		@AuthenticationPrincipal MemberPrincipal principal,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return auctionFacade.getMyBids(principal.publicId(), auctionStatus, pageable);
	}

	@GetMapping("/me/sales")
	@PreAuthorize("hasRole('SELLER')")
	public PagedResponseDto<MySaleResponseDto> getMySales(
		@AuthenticationPrincipal MemberPrincipal principal,
		@RequestParam(required = false) AuctionFilterType filter,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return auctionFacade.getMySales(principal.publicId(), filter, pageable);
	}
}