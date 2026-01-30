package com.bugzero.rarego.boundedContext.payment.out;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.boundedContext.payment.domain.SettlementFee;

public interface SettlementFeeRepository extends JpaRepository<SettlementFee, Long> {
	// SKIP LOCKED를 사용하여 다른 스레드가 처리 중인 데이터는 건너뛰고 조회
	@Query(value = """
		SELECT * FROM payment_settlement_fee
		ORDER BY id ASC 
		LIMIT :limit 
		FOR UPDATE SKIP LOCKED
		""", nativeQuery = true)
	List<SettlementFee> findAllForBatch(@Param("limit") int limit);
}
