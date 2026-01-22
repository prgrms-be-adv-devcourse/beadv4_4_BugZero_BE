package com.bugzero.rarego.boundedContext.auth.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.JwtParser;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.member.out.MemberApiClient;

@ExtendWith(MockitoExtension.class)
class AuthWithdrawAccountUseCaseTest {
	@Mock
	private JwtParser jwtParser;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private AuthSupport authSupport;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;

	@Mock
	private MemberApiClient memberApiClient;

	@InjectMocks
	private AuthWithdrawAccountUseCase authWithdrawAccountUseCase;

	@Test
	@DisplayName("access token이 없으면 AUTH_UNAUTHORIZED를 던진다.")
	void withdrawThrowsWhenAccessTokenMissing() {
		assertThatThrownBy(() -> authWithdrawAccountUseCase.withdraw(" "))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_UNAUTHORIZED);

		verifyNoInteractions(jwtParser, accountRepository, memberApiClient,
			refreshTokenRepository, authAccessTokenBlacklistUseCase, authSupport);
	}

	@Test
	@DisplayName("access token에서 publicId 추출이 실패하면 AUTH_UNAUTHORIZED를 던진다.")
	void withdrawThrowsWhenTokenInvalid() {
		String accessToken = "access-token";
		when(jwtParser.parsePrincipal(accessToken)).thenReturn(null);

		assertThatThrownBy(() -> authWithdrawAccountUseCase.withdraw(accessToken))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_UNAUTHORIZED);

		verify(jwtParser).parsePrincipal(accessToken);
		verifyNoInteractions(accountRepository, memberApiClient,
			refreshTokenRepository, authAccessTokenBlacklistUseCase, authSupport);
	}

	@Test
	@DisplayName("계정을 찾을 수 없으면 AUTH_ACCOUNT_NOT_FOUND를 던진다.")
	void withdrawThrowsWhenAccountMissing() {
		String accessToken = "access-token";
		String publicId = "member-public-id";
		when(jwtParser.parsePrincipal(accessToken)).thenReturn(new MemberPrincipal(publicId, "USER"));
		when(authSupport.findByPublicId(publicId))
			.thenThrow(new CustomException(ErrorType.AUTH_ACCOUNT_NOT_FOUND));

		assertThatThrownBy(() -> authWithdrawAccountUseCase.withdraw(accessToken))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_ACCOUNT_NOT_FOUND);

		verify(authSupport).findByPublicId(publicId);
		verifyNoInteractions(memberApiClient, refreshTokenRepository, authAccessTokenBlacklistUseCase);
	}

	@Test
	@DisplayName("이미 탈퇴한 계정이면 AUTH_ACCOUNT_DELETED를 던진다.")
	void withdrawThrowsWhenAccountDeleted() {
		String accessToken = "access-token";
		String publicId = "member-public-id";
		Account account = Account.builder()
			.memberPublicId(publicId)
			.provider(Provider.GOOGLE)
			.providerId("google-123")
			.role(AuthRole.USER)
			.build();
		account.softDelete();

		when(jwtParser.parsePrincipal(accessToken)).thenReturn(new MemberPrincipal(publicId, "USER"));
		when(authSupport.findByPublicId(publicId)).thenReturn(account);

		assertThatThrownBy(() -> authWithdrawAccountUseCase.withdraw(accessToken))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_ACCOUNT_DELETED);

		verify(authSupport).findByPublicId(publicId);
		verifyNoInteractions(memberApiClient, refreshTokenRepository, authAccessTokenBlacklistUseCase);
	}

	@Test
	@DisplayName("정상 요청이면 member/account를 탈퇴 처리하고 토큰을 폐기한다.")
	void withdrawSoftDeletesAccountAndTokens() {
		String accessToken = "access-token";
		String publicId = "member-public-id";
		Account account = Account.builder()
			.memberPublicId(publicId)
			.provider(Provider.GOOGLE)
			.providerId("google-123")
			.role(AuthRole.USER)
			.build();

		when(jwtParser.parsePrincipal(accessToken)).thenReturn(new MemberPrincipal(publicId, "USER"));
		when(authSupport.findByPublicId(publicId)).thenReturn(account);

		authWithdrawAccountUseCase.withdraw(accessToken);

		assertThat(account.isDeleted()).isTrue();
		verify(memberApiClient).withdraw(publicId);
		verify(accountRepository).save(account);
		verify(refreshTokenRepository).deleteByMemberPublicId(publicId);
		verify(authAccessTokenBlacklistUseCase).blacklist(accessToken);
	}
}
