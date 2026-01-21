package com.bugzero.rarego.boundedContext.payment.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;

public interface PaymentMemberRepository extends JpaRepository<PaymentMember, Long> {
	Optional<PaymentMember> findByPublicId(String publicId);
}
