package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.out.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationWriter {
	private final NotificationRepository notificationRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveWithIdempotency(Notification notification) {
		notificationRepository.save(notification);
	}
}
