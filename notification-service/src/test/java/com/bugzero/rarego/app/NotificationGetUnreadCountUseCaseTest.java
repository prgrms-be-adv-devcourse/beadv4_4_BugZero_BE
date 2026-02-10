package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.domain.NotificationMember;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.in.dto.NotificationUnreadCountResponseDto;
import com.bugzero.rarego.out.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationGetUnreadCountUseCaseTest {

	@Mock
	private NotificationSupport notificationSupport;

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationGetUnreadCountUseCase useCase;

	@Test
	@DisplayName("성공: 회원의 안 읽은 알림 개수를 조회하여 반환한다.")
	void getUnreadCount_success() {
		// given
		String publicId = "user-public-id-123";
		Long memberId = 10L;
		long expectedCount = 5L;

		// 1. Member Mock 설정
		NotificationMember member = mock(NotificationMember.class);
		given(member.getId()).willReturn(memberId);

		// Support가 해당 publicId로 Member를 찾는 동작 Stubbing
		given(notificationSupport.findMemberByPublicId(publicId)).willReturn(member);

		// 2. Repository Count 동작 Stubbing
		given(notificationRepository.countAllByMemberIdAndIsReadFalse(memberId))
			.willReturn(expectedCount);

		// when
		NotificationUnreadCountResponseDto response = useCase.getUnreadCount(publicId);

		// then
		// 반환된 값이 예상된 count와 일치하는지 검증
		assertThat(response).isNotNull();
		assertThat(response.count()).isEqualTo(expectedCount);

		// 메서드 호출 검증
		then(notificationSupport).should(times(1)).findMemberByPublicId(publicId);
		then(notificationRepository).should(times(1)).countAllByMemberIdAndIsReadFalse(memberId);
	}

	@Test
	@DisplayName("실패: 회원을 찾을 수 없으면 예외가 전파된다.")
	void getUnreadCount_fail_memberNotFound() {
		// given
		String publicId = "unknown-user";

		// Support에서 예외가 발생한다고 설정 (Support 내부 로직은 Support 테스트에서 검증하고, 여기선 전파 확인)
		given(notificationSupport.findMemberByPublicId(publicId))
			.willThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> useCase.getUnreadCount(publicId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.MEMBER_NOT_FOUND);

		// 예외 발생 시 리포지토리 메서드는 호출되지 않아야 함
		then(notificationRepository).shouldHaveNoInteractions();
	}
}