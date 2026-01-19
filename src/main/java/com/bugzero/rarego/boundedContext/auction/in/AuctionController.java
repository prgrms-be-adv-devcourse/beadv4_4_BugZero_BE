package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getUsername());

        return auctionFacade.createBid(
                auctionId,
                memberId,
                bidRequestDto.bidAmount().intValue()
        );
    }

    // 경매 입찰 기록 조회 (GET /api/v1/auctions/{auctionId}/bids)
    @Operation(summary = "경매 입찰 기록 조회", description = "경매 입찰 기록을 조회합니다")
    @GetMapping("/{auctionId}/bids")
    public PagedResponseDto<BidLogResponseDto> getBids(
            @PathVariable Long auctionId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return auctionFacade.getBidLogs(auctionId, pageable);
    }

    @Operation(summary = "관심 경매 등록", description = "특정 경매를 관심 목록에 추가합니다")
    @PostMapping("/{auctionId}/bookmarks")
    public SuccessResponseDto<WishlistAddResponseDto> addBookmark(
            Authentication authentication,
            @PathVariable Long auctionId
    ) {
        String memberUUID = authentication.getName();

        WishlistAddResponseDto response = auctionFacade.addBookmark(memberUUID, auctionId);
        return SuccessResponseDto.from(SuccessType.OK, response);
    }
}
