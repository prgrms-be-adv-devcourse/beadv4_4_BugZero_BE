package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.domain.NotificationType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.in.dto.NotificationResponseDto;
import com.bugzero.rarego.out.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationGetNotificationsUseCaseTest {

	@Mock
	private NotificationSupport notificationSupport;

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationGetNotificationsUseCase useCase;

	@Test
	@DisplayName("성공: onlyUnread가 false이면 '전체 알림 조회' 쿼리가 실행된다.")
	void getNotifications_all() {
		// given
		String publicId = "user-uuid-123";
		Long memberId = 1L;
		Pageable pageable = PageRequest.of(0, 10);

		// 1. Member 조회 Mocking
		NotificationMember member = mock(NotificationMember.class);
		given(member.getId()).willReturn(memberId);
		given(notificationSupport.findMemberByPublicId(publicId)).willReturn(member);

		// 2. Notification 조회 Mocking (DTO 변환을 위해 내부 필드 Stubbing 필요)
		Notification notification = createMockNotification();
		Page<Notification> notificationPage = new PageImpl<>(List.of(notification));

		// 전체 조회 메서드가 호출될 것을 예상하고 Stubbing
		given(notificationRepository.findAllByMemberId(eq(memberId), any(Pageable.class)))
			.willReturn(notificationPage);

		// when
		PagedResponseDto<NotificationResponseDto> result = useCase.getNotifications(publicId, false, pageable);

		// then
		// 1. 전체 조회 메서드가 호출되었는지 검증
		then(notificationRepository).should(times(1)).findAllByMemberId(memberId, pageable);
		// 2. 안 읽은 조회 메서드는 호출되지 않았는지 검증
		then(notificationRepository).shouldHaveNoMoreInteractions();

		// 3. 반환값 검증
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).title()).isEqualTo(NotificationType.AUCTION_WON.getDescription());
	}

	@Test
	@DisplayName("성공: onlyUnread가 true이면 '안 읽은 알림 조회' 쿼리가 실행된다.")
	void getNotifications_onlyUnread() {
		// given
		String publicId = "user-uuid-123";
		Long memberId = 1L;
		Pageable pageable = PageRequest.of(0, 10);

		NotificationMember member = mock(NotificationMember.class);
		given(member.getId()).willReturn(memberId);
		given(notificationSupport.findMemberByPublicId(publicId)).willReturn(member);

		Notification notification = createMockNotification();
		Page<Notification> notificationPage = new PageImpl<>(List.of(notification));

		// 안 읽은 알림 조회 메서드가 호출될 것을 예상하고 Stubbing
		given(notificationRepository.findAllByMemberIdAndIsReadFalse(eq(memberId), any(Pageable.class)))
			.willReturn(notificationPage);

		// when
		PagedResponseDto<NotificationResponseDto> result = useCase.getNotifications(publicId, true, pageable);

		// then
		// 1. 안 읽은 알림 조회 메서드가 호출되었는지 검증
		then(notificationRepository).should(times(1)).findAllByMemberIdAndIsReadFalse(memberId, pageable);
		// 2. 전체 조회 메서드는 호출되지 않았는지 검증
		then(notificationRepository).should(times(0)).findAllByMemberId(any(), any());
	}

	@Test
	@DisplayName("성공: onlyUnread가 null이면 기본값으로 '전체 알림 조회'가 실행된다.")
	void getNotifications_null_param() {
		// given
		String publicId = "user-uuid-123";
		Long memberId = 1L;
		Pageable pageable = PageRequest.of(0, 10);

		NotificationMember member = mock(NotificationMember.class);
		given(member.getId()).willReturn(memberId);
		given(notificationSupport.findMemberByPublicId(publicId)).willReturn(member);

		Page<Notification> emptyPage = Page.empty();
		given(notificationRepository.findAllByMemberId(eq(memberId), any(Pageable.class)))
			.willReturn(emptyPage);

		// when
		// onlyUnread에 null 전달
		useCase.getNotifications(publicId, null, pageable);

		// then
		// Boolean.TRUE.equals(null) -> false 이므로 전체 조회가 호출되어야 함
		then(notificationRepository).should(times(1)).findAllByMemberId(memberId, pageable);
	}

	// DTO 변환 과정에서 NPE를 방지하기 위해 필요한 필드들을 가진 Mock 객체 생성 헬퍼
	private Notification createMockNotification() {
		Notification notification = mock(Notification.class);
		given(notification.getId()).willReturn(100L);
		given(notification.getType()).willReturn(NotificationType.AUCTION_WON); // Enum 필수
		given(notification.getMessage()).willReturn("낙찰 축하합니다.");
		given(notification.getReferenceId()).willReturn(50L);
		given(notification.isRead()).willReturn(false);
		given(notification.getCreatedAt()).willReturn(LocalDateTime.now());
		return notification;
	}
}