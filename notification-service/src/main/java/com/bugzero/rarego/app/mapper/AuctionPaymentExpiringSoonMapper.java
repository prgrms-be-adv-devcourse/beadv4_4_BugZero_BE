package com.bugzero.rarego.app.mapper;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.NotificationSupport;
import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.domain.NotificationType;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentExpiringSoonEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuctionPaymentExpiringSoonMapper implements NotificationMapper<AuctionPaymentExpiringSoonEvent> {
	private final NotificationSupport notificationSupport;

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM월 dd일 HH시 mm분");

	@Override
	public boolean supports(Object event) {
		return event instanceof AuctionPaymentExpiringSoonEvent;
	}

	@Override
	public List<Notification> map(AuctionPaymentExpiringSoonEvent event) {
		NotificationMember buyer = notificationSupport.findMemberById(event.buyerId());
		String deadline = event.expiredAt().format(FORMATTER);

		String message = "%d번 경매의 결제 마감 시간이 임박했습니다.%n%s까지 결제를 완료해주세요."
			.formatted(event.auctionId(), deadline);

		Notification notification = Notification.builder()
			.message(message)
			.member(buyer)
			.type(NotificationType.AUCTION_PAYMENT_EXPIRING_SOON)
			.referenceId(event.orderId())
			.build();

		return List.of(notification);
	}
}
