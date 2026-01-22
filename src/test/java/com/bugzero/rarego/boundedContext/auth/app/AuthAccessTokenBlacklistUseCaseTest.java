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

import com.bugzero.rarego.boundedContext.auth.domain.AccessTokenBlacklist;
import com.bugzero.rarego.boundedContext.auth.out.AccessTokenBlacklistRepository;
import com.bugzero.rarego.global.security.JwtParser;

@ExtendWith(MockitoExtension.class)
class AuthAccessTokenBlacklistUseCaseTest {
	@Mock
	private AccessTokenBlacklistRepository accessTokenBlacklistRepository;

	@Mock
	private JwtParser jwtParser;

	@InjectMocks
	private AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;

	@Test
	@DisplayName("access token이 없으면 블랙리스트 처리를 건너뛴다.")
	void blacklistSkipsWhenTokenMissing() {
		// given
		String accessToken = " ";

		// when
		authAccessTokenBlacklistUseCase.blacklist(accessToken);

		// then
		verifyNoInteractions(jwtParser, accessTokenBlacklistRepository);
	}

	@Test
	@DisplayName("이미 만료된 access token이면 블랙리스트 처리를 건너뛴다.")
	void blacklistSkipsWhenTokenExpired() {
		// given
		String accessToken = "access-token";
		when(jwtParser.expiresAt(accessToken)).thenReturn(LocalDateTime.now().minusMinutes(1));

		// when
		authAccessTokenBlacklistUseCase.blacklist(accessToken);

		// then
		verifyNoInteractions(accessTokenBlacklistRepository);
	}

	@Test
	@DisplayName("이미 블랙리스트에 있으면 저장하지 않는다.")
	void blacklistSkipsWhenAlreadyBlacklisted() {
		// given
		String accessToken = "access-token";
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
		when(jwtParser.expiresAt(accessToken)).thenReturn(expiresAt);
		when(accessTokenBlacklistRepository.existsByAccessTokenAndExpiresAtAfter(eq(accessToken), any(LocalDateTime.class)))
			.thenReturn(true);

		// when
		authAccessTokenBlacklistUseCase.blacklist(accessToken);

		// then
		verify(accessTokenBlacklistRepository).existsByAccessTokenAndExpiresAtAfter(eq(accessToken), any(LocalDateTime.class));
		verify(accessTokenBlacklistRepository, never()).save(any(AccessTokenBlacklist.class));
	}

	@Test
	@DisplayName("유효한 access token이면 블랙리스트에 저장한다.")
	void blacklistStoresWhenTokenValid() {
		// given
		String accessToken = "access-token";
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
		when(jwtParser.expiresAt(accessToken)).thenReturn(expiresAt);
		when(accessTokenBlacklistRepository.existsByAccessTokenAndExpiresAtAfter(eq(accessToken), any(LocalDateTime.class)))
			.thenReturn(false);

		// when
		authAccessTokenBlacklistUseCase.blacklist(accessToken);

		// then
		ArgumentCaptor<AccessTokenBlacklist> captor = ArgumentCaptor.forClass(AccessTokenBlacklist.class);
		verify(accessTokenBlacklistRepository).save(captor.capture());
		AccessTokenBlacklist saved = captor.getValue();
		assertThat(saved.getAccessToken()).isEqualTo(accessToken);
		assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	@DisplayName("블랙리스트 조회는 저장소 결과를 반환한다.")
	void isBlacklistedReturnsRepositoryResult() {
		// given
		String accessToken = "access-token";
		when(accessTokenBlacklistRepository.existsByAccessTokenAndExpiresAtAfter(eq(accessToken), any(LocalDateTime.class)))
			.thenReturn(true);

		// when
		boolean result = authAccessTokenBlacklistUseCase.isBlacklisted(accessToken);

		// then
		assertThat(result).isTrue();
		verify(accessTokenBlacklistRepository).existsByAccessTokenAndExpiresAtAfter(eq(accessToken), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("만료된 블랙리스트 삭제는 삭제 개수를 반환한다.")
	void deleteExpiredReturnsDeleteCount() {
		// given
		when(accessTokenBlacklistRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(3L);

		// when
		long result = authAccessTokenBlacklistUseCase.deleteExpired();

		// then
		assertThat(result).isEqualTo(3L);
		verify(accessTokenBlacklistRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
	}
}
