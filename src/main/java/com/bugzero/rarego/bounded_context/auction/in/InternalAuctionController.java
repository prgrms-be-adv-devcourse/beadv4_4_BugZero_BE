package com.bugzero.rarego.bounded_context.auction.in;

import com.bugzero.rarego.bounded_context.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.bounded_context.auction.in.dto.AuctionAutoSettleResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/internal/auctions")
@RequiredArgsConstructor
@Tag(name = "Internal - Auction", description = "내부 경매 API (시스템 전용)")
@Hidden
public class InternalAuctionController {

    private final AuctionSettleAuctionFacade facade;

    @Operation(summary = "경매 정산", description = "종료된 경매를 정산합니다")
    @PostMapping("/settle")
    public SuccessResponseDto<AuctionAutoSettleResponseDto> settle() {
        return SuccessResponseDto.from(SuccessType.OK, facade.settle());
    }
}

