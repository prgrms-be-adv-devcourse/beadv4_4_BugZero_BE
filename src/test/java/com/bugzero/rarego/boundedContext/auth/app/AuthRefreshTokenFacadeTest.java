package com.bugzero.rarego.boundedContext.auth.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.domain.RefreshToken;
import com.bugzero.rarego.boundedContext.auth.domain.TokenPairDto;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.JwtParser;

@ExtendWith(MockitoExtension.class)
class AuthRefreshTokenFacadeTest {
	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private JwtParser jwtParser;

	@Mock
	private AuthIssueTokenUseCase authIssueTokenUseCase;

	@Mock
	private AuthStoreRefreshTokenUseCase authStoreRefreshTokenUseCase;

	@Mock
	private AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;

	@Mock
	private AccountRepository accountRepository;

	@InjectMocks
	private AuthRefreshTokenFacade authRefreshTokenFacade;

	@Test
	@DisplayName("refresh token이 없으면 AUTH_REFRESH_TOKEN_REQUIRED 예외가 발생한다.")
	void refreshFailsWhenRefreshTokenMissing() {
		// given
		String refreshToken = " ";
		String accessToken = "access-token";

		// when
		Throwable thrown = catchThrowable(() -> authRefreshTokenFacade.refresh(refreshToken, accessToken));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_REFRESH_TOKEN_REQUIRED);
		verifyNoInteractions(
			refreshTokenRepository,
			jwtParser,
			authIssueTokenUseCase,
			authStoreRefreshTokenUseCase,
			authAccessTokenBlacklistUseCase,
			accountRepository
		);
	}

	@Test
	@DisplayName("저장된 refresh token이 없으면 AUTH_REFRESH_TOKEN_INVALID 예외가 발생한다.")
	void refreshFailsWhenTokenNotFound() {
		// given
		String refreshToken = "refresh-token";
		String accessToken = "access-token";
		when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)).thenReturn(Optional.empty());

		// when
		Throwable thrown = catchThrowable(() -> authRefreshTokenFacade.refresh(refreshToken, accessToken));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_REFRESH_TOKEN_INVALID);
		verify(refreshTokenRepository).findByRefreshTokenAndRevokedFalse(refreshToken);
		verifyNoInteractions(jwtParser, authIssueTokenUseCase, authStoreRefreshTokenUseCase, authAccessTokenBlacklistUseCase, accountRepository);
	}

	@Test
	@DisplayName("refresh token이 만료되면 AUTH_REFRESH_TOKEN_EXPIRED 예외가 발생한다.")
	void refreshFailsWhenTokenExpired() {
		// given
		String refreshToken = "refresh-token";
		String accessToken = "access-token";
		RefreshToken stored = new RefreshToken(
			"member-public-id",
			refreshToken,
			LocalDateTime.now().minusMinutes(1),
			false
		);
		when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)).thenReturn(Optional.of(stored));

		// when
		Throwable thrown = catchThrowable(() -> authRefreshTokenFacade.refresh(refreshToken, accessToken));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_REFRESH_TOKEN_EXPIRED);
		verifyNoInteractions(jwtParser, authIssueTokenUseCase, authStoreRefreshTokenUseCase, authAccessTokenBlacklistUseCase, accountRepository);
	}

	@Test
	@DisplayName("refresh token 파싱 실패 시 AUTH_REFRESH_TOKEN_INVALID 예외가 발생한다.")
	void refreshFailsWhenTokenParseFails() {
		// given
		String refreshToken = "refresh-token";
		String accessToken = "access-token";
		RefreshToken stored = new RefreshToken(
			"member-public-id",
			refreshToken,
			LocalDateTime.now().plusMinutes(5),
			false
		);
		when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)).thenReturn(Optional.of(stored));
		when(jwtParser.parseRefreshPublicId(refreshToken)).thenReturn(null);
		when(jwtParser.expiresAt(refreshToken)).thenReturn(LocalDateTime.now().plusMinutes(5));

		// when
		Throwable thrown = catchThrowable(() -> authRefreshTokenFacade.refresh(refreshToken, accessToken));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_REFRESH_TOKEN_INVALID);
		verifyNoInteractions(authIssueTokenUseCase, authStoreRefreshTokenUseCase, authAccessTokenBlacklistUseCase, accountRepository);
	}

	@Test
	@DisplayName("refresh token 소유자가 다르면 AUTH_REFRESH_TOKEN_OWNER_MISMATCH 예외가 발생한다.")
	void refreshFailsWhenOwnerMismatch() {
		// given
		String refreshToken = "refresh-token";
		String accessToken = "access-token";
		RefreshToken stored = new RefreshToken(
			"member-public-id",
			refreshToken,
			LocalDateTime.now().plusMinutes(5),
			false
		);
		when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)).thenReturn(Optional.of(stored));
		when(jwtParser.parseRefreshPublicId(refreshToken)).thenReturn("other-member");
		when(jwtParser.expiresAt(refreshToken)).thenReturn(LocalDateTime.now().plusMinutes(5));

		// when
		Throwable thrown = catchThrowable(() -> authRefreshTokenFacade.refresh(refreshToken, accessToken));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_REFRESH_TOKEN_OWNER_MISMATCH);
		verifyNoInteractions(authIssueTokenUseCase, authStoreRefreshTokenUseCase, authAccessTokenBlacklistUseCase, accountRepository);
	}

	@Test
	@DisplayName("계정을 찾을 수 없으면 AUTH_REFRESH_TOKEN_INVALID 예외가 발생한다.")
	void refreshFailsWhenAccountMissing() {
		// given
		String refreshToken = "refresh-token";
		String accessToken = "access-token";
		String memberPublicId = "member-public-id";
		RefreshToken stored = new RefreshToken(
			memberPublicId,
			refreshToken,
			LocalDateTime.now().plusMinutes(5),
			false
		);
		when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)).thenReturn(Optional.of(stored));
		when(jwtParser.parseRefreshPublicId(refreshToken)).thenReturn(memberPublicId);
		when(accountRepository.findByMemberPublicId(memberPublicId)).thenReturn(Optional.empty());

		// when
		Throwable thrown = catchThrowable(() -> authRefreshTokenFacade.refresh(refreshToken, accessToken));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_REFRESH_TOKEN_INVALID);
		verify(accountRepository).findByMemberPublicId(memberPublicId);
		verifyNoInteractions(authIssueTokenUseCase, authStoreRefreshTokenUseCase, authAccessTokenBlacklistUseCase);
	}

	@Test
	@DisplayName("refresh 성공 시 기존 토큰을 폐기하고 새로운 토큰 쌍을 반환한다.")
	void refreshSucceedsAndStoresNewTokens() {
		// given
		String refreshToken = "refresh-token";
		String accessToken = "access-token";
		String memberPublicId = "member-public-id";
		RefreshToken stored = new RefreshToken(
			memberPublicId,
			refreshToken,
			LocalDateTime.now().plusMinutes(5),
			false
		);
		Account account = Account.builder()
			.memberPublicId(memberPublicId)
			.role(AuthRole.USER)
			.provider(Provider.GOOGLE)
			.providerId("google-123")
			.build();

		when(refreshTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)).thenReturn(Optional.of(stored));
		when(jwtParser.parseRefreshPublicId(refreshToken)).thenReturn(memberPublicId);
		when(accountRepository.findByMemberPublicId(memberPublicId)).thenReturn(Optional.of(account));
		when(authIssueTokenUseCase.issueToken(memberPublicId, AuthRole.USER.name(), true)).thenReturn("new-access");
		when(authIssueTokenUseCase.issueToken(memberPublicId, AuthRole.USER.name(), false)).thenReturn("new-refresh");

		// when
		TokenPairDto result = authRefreshTokenFacade.refresh(refreshToken, accessToken);

		// then
		assertThat(result.accessToken()).isEqualTo("new-access");
		assertThat(result.refreshToken()).isEqualTo("new-refresh");
		assertThat(stored.isRevoked()).isTrue();
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		assertThat(captor.getValue().isRevoked()).isTrue();
		verify(authIssueTokenUseCase).issueToken(memberPublicId, AuthRole.USER.name(), true);
		verify(authIssueTokenUseCase).issueToken(memberPublicId, AuthRole.USER.name(), false);
		verify(authStoreRefreshTokenUseCase).store(memberPublicId, "new-refresh");
		verify(authAccessTokenBlacklistUseCase).blacklist(accessToken);
	}
}
