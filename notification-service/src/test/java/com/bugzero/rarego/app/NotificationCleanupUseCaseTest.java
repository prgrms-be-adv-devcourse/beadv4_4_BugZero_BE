package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.out.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupUseCaseTest {

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationCleanupUseCase notificationCleanupUseCase;

	@Test
	@DisplayName("오래된 알림 삭제: 현재 시간 기준으로 30일 이전의 날짜를 계산하여 리포지토리를 호출한다.")
	void deleteOldNotifications_success() {
		// given
		// 테스트 실행 시점의 30일 전 시간 계산 (예상값)
		LocalDateTime expectedThreshold = LocalDateTime.now().minusDays(30);

		// when
		notificationCleanupUseCase.deleteOldNotifications();

		// then
		// 1. ArgumentCaptor를 사용하여 실제 메서드에 전달된 파라미터(날짜)를 낚아챕니다.
		ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);

		// 2. 리포지토리의 deleteByCreatedAtBefore 메서드가 정확히 1번 호출되었는지 검증하고, 파라미터를 캡처합니다.
		then(notificationRepository).should(times(1))
			.deleteByCreatedAtBefore(captor.capture());

		// 3. 캡처한 날짜가 예상값(expectedThreshold)과 비슷한지 검증합니다.
		// 테스트 실행 속도 차이를 고려하여 2초 정도의 오차는 허용합니다.
		LocalDateTime actualThreshold = captor.getValue();

		assertThat(actualThreshold).isCloseTo(expectedThreshold, within(2, ChronoUnit.SECONDS));
	}
}
