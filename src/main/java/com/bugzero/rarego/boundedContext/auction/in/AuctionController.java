package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auction API", description = "경매 상품 조회 및 입찰 관련 API")
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

	@Operation(summary = "경매 상세 조회", description = "경매의 상세 정보를 조회합니다. (로그인 시 내 입찰 내역 포함)")
	@GetMapping("/{auctionId}")
	public SuccessResponseDto<AuctionDetailResponseDto> getAuctionDetail(
		@PathVariable Long auctionId,
		@AuthenticationPrincipal MemberPrincipal principal
	) {
		String memberPublicId = (principal != null) ? principal.publicId() : null;
		return auctionFacade.getAuctionDetail(auctionId, memberPublicId);
	}

	@Operation(summary = "입찰하기", description = "특정 경매에 입찰을 진행합니다. (판매자 본인 입찰 불가)")
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

	@Operation(summary = "입찰 기록 조회", description = "해당 경매의 실시간 입찰 내역(로그)을 조회합니다.")
	@GetMapping("/{auctionId}/bids")
	public PagedResponseDto<BidLogResponseDto> getBids(
		@PathVariable Long auctionId,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return auctionFacade.getBidLogs(auctionId, pageable);
	}

	@Operation(summary = "낙찰 상세 정보 조회", description = "경매 종료 후 낙찰 정보를 조회합니다. (구매자/판매자만 조회 가능)")
	@GetMapping("/{auctionId}/order")
	public SuccessResponseDto<AuctionOrderResponseDto> getAuctionOrder(
		@PathVariable Long auctionId,
		@AuthenticationPrincipal MemberPrincipal principal
	) {
		return auctionFacade.getAuctionOrder(auctionId, principal.publicId());
	}

  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "관심 경매 등록", description = "특정 경매를 관심 목록에 추가합니다")
  @PostMapping("/{auctionId}/bookmarks")
  public SuccessResponseDto<WishlistAddResponseDto> addBookmark(
          @AuthenticationPrincipal MemberPrincipal memberPrincipal,
          @PathVariable Long auctionId
  ) {
      WishlistAddResponseDto response = auctionFacade.addBookmark(memberPrincipal.publicId(), auctionId);
      return SuccessResponseDto.from(SuccessType.OK, response);
  }
}
