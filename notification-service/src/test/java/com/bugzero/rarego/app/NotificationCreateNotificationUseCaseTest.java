package com.bugzero.rarego.app;

import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.app.mapper.NotificationMapper;
import com.bugzero.rarego.domain.Notification;
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
		// 매퍼 리스트에 Mock 매퍼 하나를 넣어서 주입
		List<NotificationMapper<?>> mappers = List.of(notificationMapper);
		useCase = new NotificationCreateNotificationUseCase(notificationRepository, mappers);
	}

	@Test
	@DisplayName("성공: 지원하는 이벤트가 들어오면 알림을 생성하고 저장한다.")
	void createNotification_success() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class); // 내부 값은 중요하지 않으므로 Mock 처리

		// 매퍼가 이 이벤트를 지원한다고 설정
		given(notificationMapper.supports(event)).willReturn(true);
		// 매퍼가 변환 결과로 알림 리스트를 반환한다고 설정
		given(notificationMapper.map(event)).willReturn(List.of(notification));

		// when
		useCase.createNotification(event);

		// then
		// 리포지토리의 saveAll이 1번 호출되었는지 검증
		then(notificationRepository).should().saveAll(List.of(notification));
	}

	@Test
	@DisplayName("실패: 지원하지 않는 이벤트가 들어오면 저장하지 않고 로그만 남긴다(무시한다).")
	void createNotification_fail_not_supported() {
		// given
		TestEvent event = new TestEvent(1L);

		// 매퍼가 이 이벤트를 지원하지 않는다고 설정 (false)
		given(notificationMapper.supports(event)).willReturn(false);

		// when
		useCase.createNotification(event);

		// then
		// map 메서드는 호출되지 않아야 함
		then(notificationMapper).should(never()).map(any());
		// 리포지토리 save는 절대 호출되지 않아야 함
		then(notificationRepository).should(never()).saveAll(any());
	}

	@Test
	@DisplayName("실패: 매퍼가 빈 리스트를 반환하면 저장하지 않는다.")
	void createNotification_fail_empty_list() {
		// given
		TestEvent event = new TestEvent(1L);

		// 지원은 하지만
		given(notificationMapper.supports(event)).willReturn(true);
		// 결과가 비어있음 (예: 조건에 맞지 않아 알림 생성 안 함)
		given(notificationMapper.map(event)).willReturn(Collections.emptyList());

		// when
		useCase.createNotification(event);

		// then
		// 리포지토리 save는 호출되지 않아야 함
		then(notificationRepository).should(never()).saveAll(any());
	}

	// 테스트용 이벤트 클래스
	record TestEvent(Long id) {
	}
}