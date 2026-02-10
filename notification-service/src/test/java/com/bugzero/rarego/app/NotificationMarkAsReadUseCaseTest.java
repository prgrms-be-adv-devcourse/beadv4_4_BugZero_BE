package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class NotificationMarkAsReadUseCaseTest {

	@Mock
	private NotificationSupport notificationSupport;

	@InjectMocks
	private NotificationMarkAsReadUseCase useCase;

	@Test
	@DisplayName("성공: 알림의 주인과 요청자가 일치하면 읽음 처리(read)를 수행한다.")
	void markAsRead_success() {
		// given
		String publicId = "user-public-id";
		Long notificationId = 100L;
		Long sameMemberId = 1L;

		// 1. 요청자(Member) Mock 설정
		NotificationMember requestMember = mock(NotificationMember.class);
		given(requestMember.getId()).willReturn(sameMemberId);

		// 2. 알림 주인(Owner) Mock 설정
		NotificationMember ownerMember = mock(NotificationMember.class);
		given(ownerMember.getId()).willReturn(sameMemberId); // ID 일치

		// 3. 알림(Notification) Mock 설정
		Notification notification = mock(Notification.class);
		given(notification.getMember()).willReturn(ownerMember); // 체이닝 주입

		// 4. Support Stubbing
		given(notificationSupport.findMemberByPublicId(publicId)).willReturn(requestMember);
		given(notificationSupport.findNotificationById(notificationId)).willReturn(notification);

		// when
		useCase.markAsRead(publicId, notificationId);

		// then
		// notification.read()가 1번 호출되었는지 검증
		then(notification).should(times(1)).read();
	}

	@Test
	@DisplayName("실패: 알림의 주인이 아니면 예외(OWNER_MISMATCH)가 발생하고 읽음 처리되지 않는다.")
	void markAsRead_fail_ownerMismatch() {
		// given
		String publicId = "user-public-id";
		Long notificationId = 100L;

		Long requesterId = 1L;
		Long ownerId = 2L; // ID 불일치

		// 1. 요청자 Mock
		NotificationMember requestMember = mock(NotificationMember.class);
		given(requestMember.getId()).willReturn(requesterId);

		// 2. 알림 주인 Mock
		NotificationMember ownerMember = mock(NotificationMember.class);
		given(ownerMember.getId()).willReturn(ownerId);

		// 3. 알림 Mock
		Notification notification = mock(Notification.class);
		given(notification.getMember()).willReturn(ownerMember);

		// 4. Support Stubbing
		given(notificationSupport.findMemberByPublicId(publicId)).willReturn(requestMember);
		given(notificationSupport.findNotificationById(notificationId)).willReturn(notification);

		// when & then
		assertThatThrownBy(() -> useCase.markAsRead(publicId, notificationId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_OWNER_MISMATCH);

		// [중요] 예외 발생 시 read()는 절대 호출되면 안 됨
		then(notification).should(never()).read();
	}
}