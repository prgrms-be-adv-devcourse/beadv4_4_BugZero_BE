package com.bugzero.rarego.boundedContext.payment.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	@EntityGraph(attributePaths = {"member"})
	Optional<Payment> findByOrderId(String orderId);
}
