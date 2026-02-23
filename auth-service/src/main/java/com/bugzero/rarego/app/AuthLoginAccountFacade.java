package com.bugzero.rarego.app;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.domain.Account;
import com.bugzero.rarego.domain.Provider;
import com.bugzero.rarego.out.AccountRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthLoginAccountFacade {
	private final AccountRepository accountRepository;
	private final AuthJoinAccountUseCase authJoinAccountUseCase;

	//
	public Account loginOrSignup(String providerId, String email, Provider provider) {
		return findByProviderAndProviderId(provider, providerId)
			.map(this::ensureNotDeleted)
			.orElseGet(() -> authJoinAccountUseCase.join(provider, providerId, email));
	}

	private Optional<Account> findByProviderAndProviderId(Provider provider, String providerId) {
		if (provider == null || providerId == null || providerId.isBlank()) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}
		return accountRepository.findByProviderAndProviderId(provider, providerId);
	}

	private Account ensureNotDeleted(Account account) {
		if (account.isDeleted()) {
			throw new CustomException(ErrorType.AUTH_ACCOUNT_DELETED);
		}
		return account;
	}
}
