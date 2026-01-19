package com.bugzero.rarego.boundedContext.payment.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;

import jakarta.persistence.LockModeType;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
	List<Settlement> findAllByStatus(SettlementStatus status);

	@Query("""
		    SELECT s
		    FROM Settlement s
		    JOIN FETCH s.seller
		    WHERE s.status = :status
		    ORDER BY s.id ASC
		""")
	List<Settlement> findAllByStatus(SettlementStatus status, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		    SELECT s
		    FROM Settlement s
		    JOIN FETCH s.seller
		    WHERE s.id = :id
		""")
	Optional<Settlement> findByIdForUpdate(Long id);

	@Query("""
		SELECT s FROM Settlement s
		WHERE s.seller.id = :sellerId
		AND (:status IS NULL OR s.status = :status)
		AND (:from IS NULL OR s.createdAt >= :from)
		AND (:to IS NULL OR s.createdAt <= :to)
		""")
	Page<Settlement> findAllBySellerIdAndStatus(Long sellerId, SettlementStatus status, LocalDateTime from,
		LocalDateTime to, Pageable pageable);
}
