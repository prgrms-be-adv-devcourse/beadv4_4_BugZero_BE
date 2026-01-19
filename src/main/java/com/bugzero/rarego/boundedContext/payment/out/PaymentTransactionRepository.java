package com.bugzero.rarego.boundedContext.payment.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
	@Query(
		"""
			SELECT pt FROM PaymentTransaction pt
			WHERE pt.member.id = :memberId
			AND (:type IS NULL OR pt.transactionType = :type)
			"""
	)
	Page<PaymentTransaction> findAllByMemberIdAndTransactionType(Long memberId,
		WalletTransactionType type, Pageable pageable);
}
