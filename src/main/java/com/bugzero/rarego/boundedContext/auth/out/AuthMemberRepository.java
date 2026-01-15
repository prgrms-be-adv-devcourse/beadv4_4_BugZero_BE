package com.bugzero.rarego.boundedContext.auth.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.auth.domain.AuthMember;

public interface AuthMemberRepository extends JpaRepository<AuthMember, Long> {
}
