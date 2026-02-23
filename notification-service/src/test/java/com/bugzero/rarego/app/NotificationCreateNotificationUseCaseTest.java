package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import com.bugzero.rarego.app.mapper.NotificationMapper;
import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.domain.NotificationType;
import com.bugzero.rarego.event.NotificationCreatedEvent;

@ExtendWith(MockitoExtension.class)
class NotificationCreateNotificationUseCaseTest {
	@Mock
	private NotificationWriter notificationWriter;

	@Mock
	private NotificationMapper<Object> notificationMapper;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private NotificationCreateNotificationUseCase useCase;

	@BeforeEach
	void setUp() {
		List<NotificationMapper<?>> mappers = List.of(notificationMapper);
		// 2. UseCase 생성 시 Repository 대신 Writer를 주입합니다.
		useCase = new NotificationCreateNotificationUseCase(notificationWriter, mappers, eventPublisher);
	}

	@Test
	@DisplayName("성공: 알림 저장 후 이벤트(NotificationCreatedEvent)가 정상적으로 발행된다.")
	void createNotification_success() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class);
		NotificationMember member = mock(NotificationMember.class);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(List.of(notification));

		given(notification.getId()).willReturn(100L);
		given(notification.getMember()).willReturn(member);
		given(member.getPublicId()).willReturn("member-uuid");
		given(notification.getType()).willReturn(NotificationType.AUCTION_WON);
		given(notification.getMessage()).willReturn("메시지");
		given(notification.getReferenceId()).willReturn(50L);
		given(notification.getCreatedAt()).willReturn(LocalDateTime.now());
		given(notification.isRead()).willReturn(false);

		// when
		useCase.createNotification(event);

		// then
		// 3. Repository의 save 대신 Writer의 saveWithIdempotency가 호출되는지 검증
		then(notificationWriter).should(times(1)).saveWithIdempotency(notification);
		then(eventPublisher).should(times(1)).publishEvent(any(NotificationCreatedEvent.class));
	}

	@Test
	@DisplayName("성공(중복무시): 중복 예외가 발생하면 예외를 catch하고 무시하며, '이벤트는 발행하지 않는다'.")
	void createNotification_success_duplicate() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class);
		NotificationMember member = mock(NotificationMember.class);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(List.of(notification));
		given(notification.getMember()).willReturn(member); // 로그 출력용 mock 세팅

		// 중복 상황을 시뮬레이션: Writer가 DataIntegrityViolationException을 던지도록 설정
		DataIntegrityViolationException duplicateException =
			new DataIntegrityViolationException("Duplicate entry '1-OUTBID' for key 'uk_notification_dedup'");

		willThrow(duplicateException)
			.given(notificationWriter).saveWithIdempotency(notification);

		// when
		useCase.createNotification(event);

		// then
		then(notificationWriter).should(times(1)).saveWithIdempotency(notification);
		// 예외가 catch되어 무시되었으므로 이벤트 발행 로직에는 도달하지 않아야 함
		then(eventPublisher).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("실패: 중복이 아닌 데이터 무결성 예외는 밖으로 던져져야 하며, 이벤트는 발행되지 않는다.")
	void createNotification_fail_integrity_violation() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(List.of(notification));

		// 중복이 아닌 다른 원인의 DataIntegrityViolationException 세팅
		DataIntegrityViolationException otherException =
			new DataIntegrityViolationException("Column 'message' cannot be null");

		willThrow(otherException)
			.given(notificationWriter).saveWithIdempotency(notification);

		// when & then
		assertThatThrownBy(() -> useCase.createNotification(event))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("Column 'message' cannot be null");

		then(eventPublisher).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("실패: 지원하지 않는 이벤트는 처리하지 않는다.")
	void createNotification_fail_not_supported() {
		// given
		TestEvent event = new TestEvent(1L);
		given(notificationMapper.supports(event)).willReturn(false);

		// when
		useCase.createNotification(event);

		// then
		then(notificationWriter).shouldHaveNoInteractions();
		then(eventPublisher).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("실패: 매퍼 결과가 비어있으면 처리하지 않는다.")
	void createNotification_fail_empty_list() {
		// given
		TestEvent event = new TestEvent(1L);
		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(Collections.emptyList());

		// when
		useCase.createNotification(event);

		// then
		then(notificationWriter).shouldHaveNoInteractions();
		then(eventPublisher).shouldHaveNoInteractions();
	}

	record TestEvent(Long id) {
	}
}
