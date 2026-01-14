package com.bugzero.rarego.boundedContext.auction.app;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuctionSettlementFacade {

    private final SettleExpiredAuctionsUseCase useCase;

    public Map<String, Object> settle() {
        return useCase.execute();
    }
}
