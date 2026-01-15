package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionAutoResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/internal/auctions")
@RequiredArgsConstructor
public class InternalAuctionController {

    private final AuctionSettleAuctionFacade facade;

    @PostMapping("/settle")
    public SuccessResponseDto<AuctionAutoResponseDto> settle() {
        return SuccessResponseDto.from(SuccessType.OK, facade.settle());
    }
}

