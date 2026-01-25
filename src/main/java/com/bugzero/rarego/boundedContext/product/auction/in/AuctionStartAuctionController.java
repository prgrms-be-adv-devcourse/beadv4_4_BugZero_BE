package com.bugzero.rarego.boundedContext.product.auction.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.auction.app.AuctionDetermineStartAuctionUseCase;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Tag(name = "Auction_Inspection", description = "경매일정 관련 API")
public class AuctionStartAuctionController {

	private final AuctionDetermineStartAuctionUseCase auctionDetermineStartAuctionUseCase;

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "검수승인 후 경매일정 확정", description = "관리자가 검수 승인 후 해당상품의 경매일정을 확정합니다.")
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/{productId}/startTime")
	public SuccessResponseDto<Long> deterMineStartAuction(
		@PathVariable Long productId
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			auctionDetermineStartAuctionUseCase.determineStartAuction(productId));
	}
}
