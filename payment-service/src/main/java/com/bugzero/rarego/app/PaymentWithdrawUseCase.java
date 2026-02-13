package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.shared.auction.out.AuctionApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentWithdrawUseCase {

	private final AuctionApiClient auctionApiClient;

	public boolean hasProcessingOrders(String publicId) {
		return auctionApiClient.hasProcessingOrders(publicId);
	}
}
