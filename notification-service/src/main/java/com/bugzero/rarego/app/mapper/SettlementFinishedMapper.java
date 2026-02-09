package com.bugzero.rarego.app.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.NotificationSupport;
import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.domain.NotificationType;
import com.bugzero.rarego.shared.payment.dto.SettlementResponseDto;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementFinishedMapper implements NotificationMapper<SettlementFinishedEvent> {
	private final NotificationSupport notificationSupport;

	@Override
	public boolean supports(Object event) {
		return event instanceof SettlementFinishedEvent;
	}

	@Override
	public List<Notification> map(SettlementFinishedEvent event) {
		return event.settlements().stream()
			.map(this::createNotificationFromDto)
			.toList();
	}

	private Notification createNotificationFromDto(SettlementResponseDto dto) {
		NotificationMember seller = notificationSupport.findMemberById(dto.sellerId());

		String message = "%d번 경매의 판매 대금 %d원이 정산되었습니다."
			.formatted(dto.auctionId(), dto.settlementAmount());

		return Notification.builder()
			.message(message)
			.member(seller)
			.type(NotificationType.SETTLEMENT_COMPLETED)
			.referenceId(dto.id())
			.build();
	}
}
