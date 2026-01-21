package com.bugzero.rarego.boundedContext.auth.app;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.auth.domain.RefreshToken;
import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class AuthLogoutAccountUseCaseTest {
	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;

	@InjectMocks
	private AuthLogoutAccountUseCase authLogoutAccountUseCase;

	@Test
	@DisplayName("refresh 토큰이 null이면 access token만 블랙리스트 처리한다.")
	void logoutBlacklistsWhenRefreshTokenNull() {
		// given
		String accessToken = "access-token";

		// when
		authLogoutAccountUseCase.logout(null, accessToken);

		// then
		verify(authAccessTokenBlacklistUseCase).blacklist(accessToken);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	@DisplayName("refresh 토큰이 공백이면 access token만 블랙리스트 처리한다.")
	void logoutBlacklistsWhenRefreshTokenBlank() {
		// given
		String accessToken = "access-token";

		// when
		authLogoutAccountUseCase.logout(" ", accessToken);

		// then
		verify(authAccessTokenBlacklistUseCase).blacklist(accessToken);
		verifyNoInteractions(refreshTokenRepository);
	}

	@Test
	@DisplayName("refresh 토큰이 있으면 저장소에서 조회 후 삭제한다.")
	void logoutDeletesRefreshTokenWhenFound() {
		// given
		String accessToken = "access-token";
		String refreshTokenValue = "refresh-token";
		RefreshToken refreshToken = new RefreshToken("member-public-id", refreshTokenValue, LocalDateTime.now().plusDays(1));
		when(refreshTokenRepository.findByRefreshToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));

		// when
		authLogoutAccountUseCase.logout(refreshTokenValue, accessToken);

		// then
		verify(authAccessTokenBlacklistUseCase).blacklist(accessToken);
		verify(refreshTokenRepository).findByRefreshToken(refreshTokenValue);
		verify(refreshTokenRepository).delete(refreshToken);
	}

	@Test
	@DisplayName("refresh 토큰이 없으면 삭제하지 않는다.")
	void logoutSkipsDeleteWhenRefreshTokenMissing() {
		// given
		String accessToken = "access-token";
		String refreshTokenValue = "refresh-token";
		when(refreshTokenRepository.findByRefreshToken(refreshTokenValue)).thenReturn(Optional.empty());

		// when
		authLogoutAccountUseCase.logout(refreshTokenValue, accessToken);

		// then
		verify(authAccessTokenBlacklistUseCase).blacklist(accessToken);
		verify(refreshTokenRepository).findByRefreshToken(refreshTokenValue);
		verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
	}
}
