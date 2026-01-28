package com.bugzero.rarego.boundedContext.payment.out;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.bugzero.rarego.boundedContext.payment.domain.SettlementFee;

import jakarta.persistence.LockModeType;

public interface SettlementFeeRepository extends JpaRepository<SettlementFee, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT sf FROM SettlementFee sf ORDER BY sf.id ASC")
	List<SettlementFee> findAllForBatch(Pageable pageable);
}
