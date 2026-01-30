package com.bugzero.rarego.bounded_context.payment.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.bounded_context.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	@EntityGraph(attributePaths = {"member"})
	Optional<Payment> findByOrderId(String orderId);
}
