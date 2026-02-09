package com.bugzero.rarego.app.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.NotificationSupport;
import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.domain.NotificationType;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuctionEndedMapper implements NotificationMapper<AuctionEndedEvent> {
	private final NotificationSupport notificationSupport;

	@Override
	public boolean supports(Object event) {
		return event instanceof AuctionEndedEvent;
	}

	@Override
	public List<Notification> map(AuctionEndedEvent event) {
		String message = "축하합니다. %d번 경매의 상품을 %d원에 낙찰받았습니다.%n결제를 진행해주세요."
			.formatted(event.auctionId(), event.finalPrice());

		NotificationMember member = notificationSupport.findMemberById(event.winnerId());

		Notification notification = Notification.builder()
			.message(message)
			.member(member)
			.type(NotificationType.AUCTION_WON)
			.referenceId(event.auctionId())
			.build();

		return List.of(notification);
	}
}
