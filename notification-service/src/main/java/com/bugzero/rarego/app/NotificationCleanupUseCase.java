package com.bugzero.rarego.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.out.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCleanupUseCase {
	private final NotificationRepository notificationRepository;

	@Transactional
	public void deleteOldNotifications() {
		// 30일 지난 알림
		LocalDateTime threshold = LocalDateTime.now().minusDays(30);

		notificationRepository.deleteByCreatedAtBefore(threshold);

		log.info("30일 지난 알림 삭제 완료");
	}
}
