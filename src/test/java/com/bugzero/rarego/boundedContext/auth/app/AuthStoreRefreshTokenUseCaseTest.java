package com.bugzero.rarego.boundedContext.auth.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auth.domain.RefreshToken;
import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class AuthStoreRefreshTokenUseCaseTest {
	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@InjectMocks
	private AuthStoreRefreshTokenUseCase authStoreRefreshTokenUseCase;

	@Test
	@DisplayName("refresh 토큰 저장 시 만료시간을 계산해 저장한다.")
	void storeSavesRefreshTokenWithExpiry() {
		// given
		ReflectionTestUtils.setField(authStoreRefreshTokenUseCase, "refreshTokenExpireSeconds", 3600);
		String memberPublicId = "550e8400-e29b-41d4-a716-446655440000";
		String refreshToken = "refresh-token";
		LocalDateTime start = LocalDateTime.now();

		// when
		authStoreRefreshTokenUseCase.store(memberPublicId, refreshToken);
		LocalDateTime end = LocalDateTime.now();

		// then
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		RefreshToken saved = captor.getValue();
		assertThat(saved.getMemberPublicId()).isEqualTo(memberPublicId);
		assertThat(saved.getRefreshToken()).isEqualTo(refreshToken);
		assertThat(saved.isRevoked()).isFalse();
		assertThat(saved.getExpiresAt())
			.isAfterOrEqualTo(start.plusSeconds(3600))
			.isBeforeOrEqualTo(end.plusSeconds(3600));
	}

	@Test
	@DisplayName("memberPublicId가 없으면 AUTH_MEMBER_REQUIRED 예외가 발생한다.")
	void storeFailsWhenMemberPublicIdMissing() {
		// given
		ReflectionTestUtils.setField(authStoreRefreshTokenUseCase, "refreshTokenExpireSeconds", 3600);

		// when
		Throwable thrown = catchThrowable(() -> authStoreRefreshTokenUseCase.store(" ", "refresh-token"));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_MEMBER_REQUIRED);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	@DisplayName("refresh 토큰이 없으면 AUTH_MEMBER_REQUIRED 예외가 발생한다.")
	void storeFailsWhenRefreshTokenMissing() {
		// given
		ReflectionTestUtils.setField(authStoreRefreshTokenUseCase, "refreshTokenExpireSeconds", 3600);

		// when
		Throwable thrown = catchThrowable(() -> authStoreRefreshTokenUseCase.store("member-public-id", " "));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_MEMBER_REQUIRED);
		verifyNoInteractions(refreshTokenRepository);
	}

}
