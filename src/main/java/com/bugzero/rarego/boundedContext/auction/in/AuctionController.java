package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionAddBookmarkResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionRemoveBookmarkResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionWithdrawResponseDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    public SuccessResponseDto<AuctionAddBookmarkResponseDto> addBookmark(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long auctionId
    ) {
        AuctionAddBookmarkResponseDto response = auctionFacade.addBookmark(memberPrincipal.publicId(), auctionId);
        return SuccessResponseDto.from(SuccessType.OK, response);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "관심 경매 해제", description = "특정 경매를 관심 목록에서 제거합니다")
    @DeleteMapping("/{auctionId}/bookmarks")
    public SuccessResponseDto<AuctionRemoveBookmarkResponseDto> removeBookmark(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long auctionId
    ) {
        AuctionRemoveBookmarkResponseDto response = auctionFacade.removeBookmark(memberPrincipal.publicId(), auctionId);
        return SuccessResponseDto.from(SuccessType.OK, response);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "판매 포기", description = "경매 실패/유찰 시 상품을 더 이상 경매에 올리지 않습니다.")
    @PostMapping("/{auctionId}/withdraw")
    public SuccessResponseDto<AuctionWithdrawResponseDto> withdraw(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        AuctionWithdrawResponseDto response = auctionFacade.withdraw(auctionId, principal.publicId());
        return SuccessResponseDto.from(SuccessType.OK, response);
    }

    @Operation(summary = "재경매 등록", description = "유찰되거나 결제 실패한 경매 상품을 다시 등록합니다. (판매자 전용)")
    @PostMapping("/{auctionId}/relist")
    public SuccessResponseDto<AuctionRelistResponseDto> relistAuction(
            @PathVariable Long auctionId,
            @RequestBody AuctionRelistRequestDto request,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return auctionFacade.relistAuction(auctionId, principal.publicId(), request);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "검수승인 후 경매일정 확정", description = "관리자가 검수 승인 후 해당상품의 경매일정을 확정합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{productId}/startTime")
    public SuccessResponseDto<Long> deterMineStartAuction(
            @PathVariable Long productId
    ) {
        return SuccessResponseDto.from(SuccessType.OK,
                auctionFacade.determineStartAuction(productId));
    }

}
