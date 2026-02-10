package com.bugzero.rarego.app;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.in.dto.NotificationResponseDto;
import com.bugzero.rarego.out.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationGetNotificationsUseCase {
	private final NotificationSupport notificationSupport;
	private final NotificationRepository notificationRepository;

	public PagedResponseDto<NotificationResponseDto> getNotifications(String publicId, Boolean onlyUnread,
		Pageable pageable) {

		NotificationMember member = notificationSupport.findMemberByPublicId(publicId);

		Page<Notification> notifications = fetchNotifications(member.getId(), onlyUnread, pageable);

		return PagedResponseDto.from(notifications.map(NotificationResponseDto::from));
	}

	private Page<Notification> fetchNotifications(Long memberId, Boolean onlyUnread, Pageable pageable) {
		if (Boolean.TRUE.equals(onlyUnread)) {
			return notificationRepository.findAllByMemberIdAndIsReadFalse(memberId, pageable);
		}
		return notificationRepository.findAllByMemberId(memberId, pageable);
	}
}
