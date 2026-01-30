package com.bugzero.rarego.bounded_context.auth.app;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.bounded_context.auth.domain.Account;
import com.bugzero.rarego.bounded_context.auth.domain.Provider;
import com.bugzero.rarego.bounded_context.auth.out.AccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthFindAccountUseCase {
	private final AccountRepository accountRepository;

	public Optional<Account> findByProviderAndProviderId(Provider provider, String providerId) {
		if (provider == null || providerId == null || providerId.isBlank()) {
			return Optional.empty();
		}
		return accountRepository.findByProviderAndProviderId(provider, providerId);
	}
}
