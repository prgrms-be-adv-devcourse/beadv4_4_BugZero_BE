package com.bugzero.rarego.boundedContext.payment.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
}
