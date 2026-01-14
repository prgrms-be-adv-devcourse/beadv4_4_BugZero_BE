package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionAutoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionSettlementFacade {

    private final SettleExpiredAuctionsUseCase useCase;

    public AuctionAutoResponseDto settle() {
        return useCase.execute();
    }
}