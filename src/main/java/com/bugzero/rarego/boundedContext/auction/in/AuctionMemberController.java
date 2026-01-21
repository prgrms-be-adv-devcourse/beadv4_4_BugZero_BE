package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.MyAuctionOrderListResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auction Member API", description = "회원별 경매 활동(내역) 조회 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class AuctionMemberController {

	private final AuctionFacade auctionFacade;

	@Operation(summary = "나의 입찰 내역 조회", description = "내가 참여한 경매 입찰 목록을 상태별로 조회합니다.")
	@GetMapping("/me/bids")
	public PagedResponseDto<MyBidResponseDto> getMyBids(
		@RequestParam(required = false) AuctionStatus auctionStatus,
		@AuthenticationPrincipal MemberPrincipal principal,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return auctionFacade.getMyBids(principal.publicId(), auctionStatus, pageable);
	}

	@Operation(summary = "나의 판매 내역 조회", description = "내가 등록한 경매 물품 목록을 필터(진행중/종료 등)에 따라 조회합니다. (판매자 권한 필요)")
	@GetMapping("/me/sales")
	@PreAuthorize("hasRole('SELLER')")
	public PagedResponseDto<MySaleResponseDto> getMySales(
		@AuthenticationPrincipal MemberPrincipal principal,
		@RequestParam(required = false) AuctionFilterType filter,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return auctionFacade.getMySales(principal.publicId(), filter, pageable);
	}

	@Operation(summary = "나의 낙찰(주문) 목록 조회", description = "내가 낙찰받은 경매 목록을 결제 상태(대기/완료)에 따라 조회합니다.")
	@GetMapping("/me/orders")
	public PagedResponseDto<MyAuctionOrderListResponseDto> getMyAuctionOrders(
		@RequestParam(required = false) AuctionOrderStatus status,
		@AuthenticationPrincipal MemberPrincipal principal,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return auctionFacade.getMyAuctionOrders(principal.publicId(), status, pageable);
	}
}
