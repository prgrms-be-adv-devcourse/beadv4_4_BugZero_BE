package com.bugzero.rarego.boundedContext.auth.app;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
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
			.orElseGet(() -> authJoinAccountUseCase.join(provider, providerId, email));
	}

	private Optional<Account> findByProviderAndProviderId(Provider provider, String providerId) {
		if (provider == null || providerId == null || providerId.isBlank()) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}
		return accountRepository.findByProviderAndProviderId(provider, providerId);
	}
}
