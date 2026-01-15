package com.bugzero.rarego.boundedContext.auth.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;

public interface AccountRepository extends JpaRepository<Account, Long> {
	Optional<Account> findByProviderAndProviderId(Provider provider, String providerId);
}
