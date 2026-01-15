package com.bugzero.rarego.boundedContext.payment.out;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;

import jakarta.persistence.LockModeType;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findAllByStatus(SettlementStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		    SELECT s
		    FROM Settlement s
		    JOIN FETCH s.seller
		    WHERE s.status = :status
		    ORDER BY s.id ASC
		""")
	List<Settlement> findAllByStatus(SettlementStatus status, Pageable pageable);
}
