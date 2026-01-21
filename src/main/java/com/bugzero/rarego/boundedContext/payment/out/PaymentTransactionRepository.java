package com.bugzero.rarego.boundedContext.payment.out;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
	@Query("""
		SELECT pt FROM PaymentTransaction pt
		WHERE pt.member.id = :memberId
		AND (:type IS NULL OR pt.transactionType = :type)
		AND (:from IS NULL OR pt.createdAt >= :from)
		AND (:to IS NULL OR pt.createdAt < :to)
		"""
	)
	Page<PaymentTransaction> searchPaymentTransactions(Long memberId, WalletTransactionType type,
		LocalDateTime from, LocalDateTime to, Pageable pageable);
}
