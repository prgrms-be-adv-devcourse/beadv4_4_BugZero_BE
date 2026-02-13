package com.bugzero.rarego.in;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.NotificationFacade;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {
	private final NotificationFacade notificationFacade;

	// 매일 새벽 3시에 실행
	@Scheduled(cron = "0 0 3 * * *")
	public void runCleanUp() {
		notificationFacade.deleteOldNotifications();
	}
}
