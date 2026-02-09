package com.bugzero.rarego.app.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.NotificationSupport;
import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.domain.NotificationType;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentCompletedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuctionPaymentCompletedMapper implements NotificationMapper<AuctionPaymentCompletedEvent> {
	private final NotificationSupport notificationSupport;

	@Override
	public boolean supports(Object event) {
		return event instanceof AuctionPaymentCompletedEvent;
	}

	@Override
	public List<Notification> map(AuctionPaymentCompletedEvent event) {
		NotificationMember buyer = notificationSupport.findMemberById(event.buyerId());
		NotificationMember seller = notificationSupport.findMemberById(event.sellerId());

		String message = "%s님이 %d번 경매의 결제를 완료했습니다.%n판매 대금은 정산 이후 지급됩니다."
			.formatted(buyer.getNickname(), event.auctionId());

		Notification notification = Notification.builder()
			.message(message)
			.member(seller)
			.type(NotificationType.AUCTION_PAYMENT_COMPLETED)
			.referenceId(event.auctionId())
			.build();

		return List.of(notification);
	}
}
