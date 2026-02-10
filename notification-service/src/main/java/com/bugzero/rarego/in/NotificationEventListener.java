package com.bugzero.rarego.in;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.app.NotificationSseSupport;
import com.bugzero.rarego.event.NotificationCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
	private final NotificationSseSupport notificationSseSupport;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
		notificationSseSupport.send(event.publicId(), event.response());
	}
}
