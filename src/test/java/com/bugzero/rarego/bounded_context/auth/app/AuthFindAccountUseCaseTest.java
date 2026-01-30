package com.bugzero.rarego.bounded_context.auth.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.bounded_context.auth.domain.Account;
import com.bugzero.rarego.bounded_context.auth.domain.AuthRole;
import com.bugzero.rarego.bounded_context.auth.domain.Provider;
import com.bugzero.rarego.bounded_context.auth.out.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AuthFindAccountUseCaseTest {
	@Mock
	private AccountRepository accountRepository;

	@InjectMocks
	private AuthFindAccountUseCase authFindAccountUseCase;

	@Test
	@DisplayName("provider가 null이면 빈 Optional을 반환하고 저장소를 호출하지 않는다.")
	void returnsEmptyWhenProviderNull() {
		Optional<Account> result = authFindAccountUseCase.findByProviderAndProviderId(null, "provider-id");

		assertThat(result).isEmpty();
		verifyNoInteractions(accountRepository);
	}

	@Test
	@DisplayName("providerId가 공백이면 빈 Optional을 반환하고 저장소를 호출하지 않는다.")
	void returnsEmptyWhenProviderIdBlank() {
		Optional<Account> result = authFindAccountUseCase.findByProviderAndProviderId(Provider.KAKAO, "   ");

		assertThat(result).isEmpty();
		verifyNoInteractions(accountRepository);
	}

	@Test
	@DisplayName("provider/providerId가 유효하면 저장소 조회 결과를 그대로 반환한다.")
	void delegatesToRepositoryWhenInputValid() {
		Account account = Account.builder()
			.provider(Provider.GOOGLE)
			.providerId("google-123")
			.memberPublicId("550e8400-e29b-41d4-a716-446655440000")
			.role(AuthRole.USER)
			.build();

		when(accountRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-123"))
			.thenReturn(Optional.of(account));

		Optional<Account> result = authFindAccountUseCase.findByProviderAndProviderId(Provider.GOOGLE, "google-123");

		assertThat(result).contains(account);
		verify(accountRepository).findByProviderAndProviderId(Provider.GOOGLE, "google-123");
	}
}
