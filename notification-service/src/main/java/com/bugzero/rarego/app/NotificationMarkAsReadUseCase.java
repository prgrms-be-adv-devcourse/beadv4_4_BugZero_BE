package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationMarkAsReadUseCase {
	private final NotificationSupport notificationSupport;

	@Transactional
	public void markAsRead(String publicId, Long id) {
		NotificationMember member = notificationSupport.findMemberByPublicId(publicId);

		Notification notification = notificationSupport.findNotificationById(id);

		if (!notification.getMember().getId().equals(member.getId())) {
			throw new CustomException(ErrorType.NOTIFICATION_OWNER_MISMATCH);
		}

		notification.read();
	}
}
