package com.bugzero.rarego.bounded_context.payment.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.bounded_context.payment.domain.PaymentMember;

public interface PaymentMemberRepository extends JpaRepository<PaymentMember, Long> {
	Optional<PaymentMember> findByPublicId(String publicId);
}
