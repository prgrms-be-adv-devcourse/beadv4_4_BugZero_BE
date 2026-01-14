package com.bugzero.rarego.boundedContext.payment.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
