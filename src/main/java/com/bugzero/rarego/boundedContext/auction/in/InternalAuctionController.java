package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionAutoSettleResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/internal/auctions")
@RequiredArgsConstructor
@Tag(name = "Internal - Auction", description = "내부 경매 API (시스템 전용)")
@Hidden
public class InternalAuctionController {

    private final AuctionSettleAuctionFacade facade;
    private final AuctionFacade auctionFacade;

    @Operation(summary = "경매 정산", description = "종료된 경매를 정산합니다")
    @PostMapping("/settle")
    public SuccessResponseDto<AuctionAutoSettleResponseDto> settle() {
        return SuccessResponseDto.from(SuccessType.OK, facade.settle());
    }

    @Operation(summary = "진행 중인 입찰이 있는지 확인", description = "진행 중인 입찰이 있는지 확인합니다")
    @GetMapping("/members/{publicId}/bids/active")
    public SuccessResponseDto<Boolean> hasActiveBids(@PathVariable String publicId) {
        return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasActiveBids(publicId));
    }

    @Operation(summary = "진행 중인 판매가 있는지 확인", description = "진행 중인 판매가 있는지 확인합니다")
    @GetMapping("/members/{publicId}/sales/active")
    public SuccessResponseDto<Boolean> hasActiveSales(@PathVariable String publicId) {
        return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasActiveSales(publicId));
    }

    @GetMapping("/members/{publicId}/orders/processing")
    public SuccessResponseDto<Boolean> hasProcessingOrders(@PathVariable String publicId) {
        return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasProcessingOrders(publicId));
    }
}

