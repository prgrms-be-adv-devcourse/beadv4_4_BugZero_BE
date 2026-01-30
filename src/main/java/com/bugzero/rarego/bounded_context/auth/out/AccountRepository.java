package com.bugzero.rarego.bounded_context.auth.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.bounded_context.auth.domain.Account;
import com.bugzero.rarego.bounded_context.auth.domain.Provider;

public interface AccountRepository extends JpaRepository<Account, Long> {
	Optional<Account> findByProviderAndProviderId(Provider provider, String providerId);
	Optional<Account> findByMemberPublicId(String memberPublicId);

	boolean existsByProviderAndProviderId(Provider provider, String providerId);
}
