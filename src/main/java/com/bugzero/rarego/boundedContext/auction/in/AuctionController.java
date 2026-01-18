package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidRequestDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        // TODO: 실제 SecurityContext에서 ID 추출
        // Long memberId = Long.valueOf(userDetails.getUsername());
        // testerId가 들어오면 걔를 쓰고, 없으면 2L 사용 (나중엔 userDetails 사용)
        Long memberId = (bidRequestDto.testerId() != null) ? bidRequestDto.testerId() : 2L;

        SuccessResponseDto<BidResponseDto> response = auctionFacade.createBid(
                auctionId,
                memberId,
                bidRequestDto.bidAmount().intValue()
        );

        return response;
    }
}
