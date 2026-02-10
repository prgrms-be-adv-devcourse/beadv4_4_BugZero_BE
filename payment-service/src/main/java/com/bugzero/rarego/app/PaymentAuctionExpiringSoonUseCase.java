package com.bugzero.rarego.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentExpiringSoonEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentAuctionExpiringSoonUseCase {
	private final EventPublisher eventPublisher;

	@Transactional
	public void publishExpiringSoonEvent(AuctionOrderDto order, LocalDateTime expiredAt) {
		AuctionPaymentExpiringSoonEvent event = new AuctionPaymentExpiringSoonEvent(
			order.orderId(),
			order.auctionId(),
			order.bidderId(),
			order.sellerId(),
			order.finalPrice(),
			expiredAt
		);

		eventPublisher.publish(event);
	}
}
