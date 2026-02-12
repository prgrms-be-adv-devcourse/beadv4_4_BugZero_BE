package com.bugzero.rarego.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.domain.Payment;
import com.bugzero.rarego.domain.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	@EntityGraph(attributePaths = {"member"})
	Optional<Payment> findByOrderId(String orderId);

	List<Payment> findAllByStatusAndCreatedAtBetween(PaymentStatus paymentStatus, LocalDateTime start,
		LocalDateTime end);
}
