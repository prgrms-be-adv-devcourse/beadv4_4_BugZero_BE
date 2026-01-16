package com.bugzero.rarego.boundedContext.auth.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;

@ExtendWith(MockitoExtension.class)
class AuthLoginAccountFacadeTest {
	@Mock
	private AuthFindAccountUseCase authFindAccountUseCase;

	@Mock
	private AuthJoinAccountUseCase authJoinAccountUseCase;

	@Mock
	private AuthIssueTokenUseCase authIssueTokenUseCase;

	@InjectMocks
	private AuthLoginAccountFacade authLoginAccountFacade;

	@Test
	@DisplayName("기존 계정이 있으면 가입 없이 토큰을 발급한다.")
	void loginUsesExistingAccount() {
		Account existing = Account.builder()
			.provider(Provider.GOOGLE)
			.providerId("google-123")
			.memberPublicId("550e8400-e29b-41d4-a716-446655440000")
			.role(AuthRole.USER)
			.build();

		when(authFindAccountUseCase.findByProviderAndProviderId(Provider.GOOGLE, "google-123"))
			.thenReturn(Optional.of(existing));
		when(authIssueTokenUseCase.issueToken(any(TokenIssueDto.class), eq(true)))
			.thenReturn("token");

		String token = authLoginAccountFacade.loginOrSignup(Provider.GOOGLE, "google-123");

		assertThat(token).isEqualTo("token");
		verify(authJoinAccountUseCase, never()).join(any(), anyString());
		verify(authFindAccountUseCase).findByProviderAndProviderId(Provider.GOOGLE, "google-123");
		verify(authIssueTokenUseCase).issueToken(argThat(dto ->
			existing.getMemberPublicId().equals(dto.memberPublicId())
				&& existing.getRole().name().equals(dto.role())
		), eq(true));
	}

	@Test
	@DisplayName("계정이 없으면 가입 후 토큰을 발급한다.")
	void loginCreatesAccountWhenMissing() {
		Account created = Account.builder()
			.provider(Provider.KAKAO)
			.providerId("kakao-456")
			.memberPublicId("1e2c1e52-7e77-4f5d-8c4f-1a2a12b7f9aa")
			.role(AuthRole.SELLER)
			.build();

		when(authFindAccountUseCase.findByProviderAndProviderId(Provider.KAKAO, "kakao-456"))
			.thenReturn(Optional.empty());
		when(authJoinAccountUseCase.join(Provider.KAKAO, "kakao-456"))
			.thenReturn(created);
		when(authIssueTokenUseCase.issueToken(any(TokenIssueDto.class), eq(true)))
			.thenReturn("token");

		String token = authLoginAccountFacade.loginOrSignup(Provider.KAKAO, "kakao-456");

		ArgumentCaptor<TokenIssueDto> captor = ArgumentCaptor.forClass(TokenIssueDto.class);

		assertThat(token).isEqualTo("token");
		verify(authJoinAccountUseCase).join(Provider.KAKAO, "kakao-456");
		verify(authIssueTokenUseCase).issueToken(captor.capture(), eq(true));
		assertThat(captor.getValue().memberPublicId()).isEqualTo(created.getMemberPublicId());
		assertThat(captor.getValue().role()).isEqualTo(created.getRole().name());
	}
}
