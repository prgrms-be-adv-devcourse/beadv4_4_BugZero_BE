package com.bugzero.rarego.boundedContext.auth.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.auth.domain.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
