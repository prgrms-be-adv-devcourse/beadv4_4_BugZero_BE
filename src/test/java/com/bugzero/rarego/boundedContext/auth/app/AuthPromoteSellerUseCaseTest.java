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
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class AuthPromoteSellerUseCaseTest {

	@Mock
	private AuthSupport authSupport;

	@Mock
	private AccountRepository accountRepository;

	@InjectMocks
	private AuthPromoteSellerUseCase authPromoteSellerUseCase;

	@Test
	@DisplayName("memberPublicId가 비어있으면 AUTH_MEMBER_REQUIRED 예외가 발생한다")
	void promoteSeller_rejectsBlankMemberPublicId() {
		assertThatThrownBy(() -> authPromoteSellerUseCase.promoteSeller("  "))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_MEMBER_REQUIRED);

		verifyNoInteractions(authSupport, accountRepository);
	}

	@Test
	@DisplayName("USER라면 SELLER로 변경한다")
	void promoteSeller_changesRole() {
		Account account = buildAccount(AuthRole.USER);
		when(authSupport.findByPublicId("public-id")).thenReturn(account);

		authPromoteSellerUseCase.promoteSeller("public-id");

		assertThat(account.getRole()).isEqualTo(AuthRole.SELLER);
		verifyNoInteractions(accountRepository);
	}

	@Test
	@DisplayName("이미 SELLER라면 아무것도 하지 않는다")
	void promoteSeller_returnsWhenAlreadySeller() {
		Account account = buildAccount(AuthRole.SELLER);
		when(authSupport.findByPublicId("public-id")).thenReturn(account);

		authPromoteSellerUseCase.promoteSeller("public-id");

		assertThat(account.getRole()).isEqualTo(AuthRole.SELLER);
		verifyNoInteractions(accountRepository);
	}

	@Test
	@DisplayName("이미 ADMIN이라면 아무것도 하지 않는다")
	void promoteSeller_returnsWhenAdmin() {
		Account account = buildAccount(AuthRole.ADMIN);
		when(authSupport.findByPublicId("public-id")).thenReturn(account);

		authPromoteSellerUseCase.promoteSeller("public-id");

		assertThat(account.getRole()).isEqualTo(AuthRole.ADMIN);
		verifyNoInteractions(accountRepository);
	}

	private Account buildAccount(AuthRole role) {
		return Account.builder()
			.memberPublicId("public-id")
			.role(role)
			.provider(Provider.GOOGLE)
			.providerId("google-123")
			.build();
	}
}
