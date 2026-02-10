package com.bugzero.rarego.app;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.NotificationMemberRepository;
import com.bugzero.rarego.out.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationSupport {
	private final NotificationRepository notificationRepository;
	private final NotificationMemberRepository notificationMemberRepository;

	@Transactional(readOnly = true)
	public NotificationMember findMemberById(Long memberId) {
		return notificationMemberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public NotificationMember findMemberByPublicId(String publicId) {
		return notificationMemberRepository.findByPublicId(publicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public Notification findNotificationById(Long id) {
		return notificationRepository.findById(id)
			.orElseThrow(() -> new CustomException(ErrorType.NOTIFICATION_NOT_FOUND));
	}
}
