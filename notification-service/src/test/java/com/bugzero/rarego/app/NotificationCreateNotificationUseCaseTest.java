package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.bugzero.rarego.app.mapper.NotificationMapper;
import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.out.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationCreateNotificationUseCaseTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private NotificationMapper<Object> notificationMapper;

	private NotificationCreateNotificationUseCase useCase;

	@BeforeEach
	void setUp() {
		List<NotificationMapper<?>> mappers = List.of(notificationMapper);
		useCase = new NotificationCreateNotificationUseCase(notificationRepository, mappers);
	}

	@Test
	@DisplayName("성공: 지원하는 이벤트가 들어오면 알림을 변환하고 '즉시 저장(saveAndFlush)'한다.")
	void createNotification_success() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(List.of(notification));

		// when
		useCase.createNotification(event);

		// then
		// [변경] 트랜잭션 내 예외 포착을 위해 saveAndFlush 호출 검증
		then(notificationRepository).should(times(1)).saveAndFlush(notification);
	}

	@Test
	@DisplayName("성공(중복무시): 'Duplicate entry' 예외가 발생하면 로그를 남기고 정상 종료한다.")
	void createNotification_success_duplicate() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class);
		NotificationMember member = mock(NotificationMember.class);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(List.of(notification));

		// 로그 출력을 위한 Mock Stubbing
		given(notification.getMember()).willReturn(member);

		// [핵심] 예외 메시지에 "Duplicate entry"가 포함되어야 로직에서 중복으로 인식함
		DataIntegrityViolationException duplicateException =
			new DataIntegrityViolationException("Duplicate entry '1-OUTBID' for key 'uk_notification_dedup'");

		willThrow(duplicateException)
			.given(notificationRepository).saveAndFlush(notification);

		// when
		useCase.createNotification(event);

		// then
		// 예외가 던져지지 않고(Swallowed), 저장 시도는 했음을 검증
		then(notificationRepository).should(times(1)).saveAndFlush(notification);
	}

	@Test
	@DisplayName("실패: 중복이 아닌 다른 데이터 무결성 예외(FK, NotNull 등)는 다시 던져야 한다.")
	void createNotification_fail_integrity_violation() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(List.of(notification));

		// [핵심] 중복 키워드가 없는 다른 종류의 예외 생성
		DataIntegrityViolationException otherException =
			new DataIntegrityViolationException("Column 'message' cannot be null");

		willThrow(otherException)
			.given(notificationRepository).saveAndFlush(notification);

		// when & then
		// 중복이 아니므로 예외가 밖으로 던져져야 함 -> Kafka 재시도 유도
		assertThatThrownBy(() -> useCase.createNotification(event))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("cannot be null");

		then(notificationRepository).should(times(1)).saveAndFlush(notification);
	}

	@Test
	@DisplayName("실패: 지원하지 않는 이벤트가 들어오면 저장하지 않는다.")
	void createNotification_fail_not_supported() {
		// given
		TestEvent event = new TestEvent(1L);

		given(notificationMapper.supports(event)).willReturn(false);

		// when
		useCase.createNotification(event);

		// then
		then(notificationMapper).should(never()).map(any());
		then(notificationRepository).should(never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("실패: 매퍼가 빈 리스트를 반환하면 저장하지 않는다.")
	void createNotification_fail_empty_list() {
		// given
		TestEvent event = new TestEvent(1L);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(Collections.emptyList());

		// when
		useCase.createNotification(event);

		// then
		then(notificationRepository).should(never()).saveAndFlush(any());
	}

	record TestEvent(Long id) {
	}
}