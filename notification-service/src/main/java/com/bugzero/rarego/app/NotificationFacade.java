package com.bugzero.rarego.app;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.shared.member.domain.MemberDto;

import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.in.dto.NotificationResponseDto;
import com.bugzero.rarego.in.dto.NotificationUnreadCountResponseDto;

import com.bugzero.rarego.shared.member.domain.MemberDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationFacade {
	private final NotificationCreateNotificationUseCase notificationCreateNotificationUseCase;
	private final NotificationSyncMemberUseCase notificationSyncMemberUseCase;
	private final NotificationGetNotificationsUseCase notificationGetNotificationsUseCase;
	private final NotificationGetUnreadCountUseCase notificationGetUnreadCountUseCase;
	private final NotificationMarkAsReadUseCase notificationMarkAsReadUseCase;

	public void createNotification(Object event) {
		notificationCreateNotificationUseCase.createNotification(event);
	}
    
	public PagedResponseDto<NotificationResponseDto> getNotifications(String publicId, Boolean onlyUnread,
		Pageable pageable) {
		return notificationGetNotificationsUseCase.getNotifications(publicId, onlyUnread, pageable);
	}

	public NotificationUnreadCountResponseDto getUnreadCount(String publicId) {
		return notificationGetUnreadCountUseCase.getUnreadCount(publicId);
	}

	public void markAsRead(String publicId, Long id) {
		notificationMarkAsReadUseCase.markAsRead(publicId, id);
	}

	public void syncMember(MemberDto memberDto) {
		notificationSyncMemberUseCase.syncMember(memberDto);
	}
}
