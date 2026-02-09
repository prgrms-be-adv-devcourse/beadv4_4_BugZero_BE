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
	@DisplayName("성공: 지원하는 이벤트가 들어오면 알림을 생성하고 '단건으로' 저장한다.")
	void createNotification_success() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(List.of(notification));

		// when
		useCase.createNotification(event);

		// then
		// [변경] saveAll이 아니라 save가 호출되었는지 검증
		then(notificationRepository).should(times(1)).save(notification);
	}

	@Test
	@DisplayName("성공(중복무시): 이미 존재하는 알림(중복)이라면 에러를 무시하고 정상 종료한다.")
	void createNotification_success_duplicate() {
		// given
		TestEvent event = new TestEvent(1L);
		Notification notification = mock(Notification.class);
		NotificationMember member = mock(NotificationMember.class);

		given(notificationMapper.supports(event)).willReturn(true);
		given(notificationMapper.map(event)).willReturn(List.of(notification));

		// [중요] 예외 발생 시 로그를 찍기 위해 notification.getMember().getId()를 호출함.
		// Mock 객체이므로 NullPointerException 방지를 위해 Member 스텁핑 필요
		given(notification.getMember()).willReturn(member);

		// [핵심] 저장 시 DataIntegrityViolationException 예외가 터지도록 설정
		willThrow(new DataIntegrityViolationException("Duplicate entry"))
			.given(notificationRepository).save(notification);

		// when
		// 예외가 던져지지 않아야 테스트 통과 (try-catch 작동 확인)
		useCase.createNotification(event);

		// then
		// 저장은 시도했으나 예외를 삼켰음을 검증
		then(notificationRepository).should(times(1)).save(notification);
	}

	@Test
	@DisplayName("실패: 지원하지 않는 이벤트가 들어오면 저장하지 않고 로그만 남긴다(무시한다).")
	void createNotification_fail_not_supported() {
		// given
		TestEvent event = new TestEvent(1L);

		given(notificationMapper.supports(event)).willReturn(false);

		// when
		useCase.createNotification(event);

		// then
		then(notificationMapper).should(never()).map(any());
		// [변경] saveAll -> save
		then(notificationRepository).should(never()).save(any());
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
		// [변경] saveAll -> save
		then(notificationRepository).should(never()).save(any());
	}

	record TestEvent(Long id) {
	}
}