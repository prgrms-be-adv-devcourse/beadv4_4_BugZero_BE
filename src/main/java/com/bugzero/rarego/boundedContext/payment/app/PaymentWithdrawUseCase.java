package com.bugzero.rarego.boundedContext.payment.app;

import com.bugzero.rarego.shared.auction.out.AuctionApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentWithdrawUseCase {

    private final AuctionApiClient auctionApiClient;

    public boolean hasProcessingOrders(String publicId) {
        return auctionApiClient.hasProcessingOrders(publicId);
    }
}