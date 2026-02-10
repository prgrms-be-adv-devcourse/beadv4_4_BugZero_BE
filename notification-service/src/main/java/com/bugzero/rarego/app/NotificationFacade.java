package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationFacade {
	private final NotificationCreateNotificationUseCase notificationCreateNotificationUseCase;

	public void createNotification(Object event) {
		notificationCreateNotificationUseCase.createNotification(event);
	}
}
