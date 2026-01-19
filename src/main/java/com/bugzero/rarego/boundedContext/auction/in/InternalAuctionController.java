package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionAutoSettleResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionPaymentTimeoutResponse;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/internal/auctions")
@RequiredArgsConstructor
@Tag(name = "Internal - Auction", description = "내부 경매 API (시스템 전용)")
@Hidden
@Slf4j
public class InternalAuctionController {

    private final AuctionSettleAuctionFacade facade;
    private final AuctionFacade auctionFacade;

    @Operation(summary = "경매 정산", description = "종료된 경매를 정산합니다")
    @PostMapping("/settle")
    public SuccessResponseDto<AuctionAutoSettleResponseDto> settle() {
        return SuccessResponseDto.from(SuccessType.OK, facade.settle());
    }

    @Operation(summary = "낙찰 결제 타임아웃 처리",
            description = "24시간 내 미결제 주문을 취소하고 보증금을 몰수합니다")
    @PostMapping("/timeout")
    public SuccessResponseDto<AuctionPaymentTimeoutResponse> processPaymentTimeout() {
        return SuccessResponseDto.from(SuccessType.OK, auctionFacade.processPaymentTimeout());
    }
}

