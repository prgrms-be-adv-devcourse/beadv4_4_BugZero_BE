package com.bugzero.rarego.boundedContext.auth.app;

import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthSupport {
	private final AccountRepository accountRepository;

	public boolean existsByProviderAndProviderId(String provider, String providerId) {
		Provider provider1 = Provider.valueOf(provider.toUpperCase());
		return accountRepository.existsByProviderAndProviderId(provider1, providerId);
	}


}
