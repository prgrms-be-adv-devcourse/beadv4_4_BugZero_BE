package com.bugzero.rarego.out;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.domain.AuctionOutbox;
import com.bugzero.rarego.domain.AuctionOutboxStatus;

public interface AuctionOutboxRepository extends JpaRepository<AuctionOutbox, Long> {
	@Query(value = """
		SELECT * FROM AUCTION_OUTBOX o
		WHERE o.status = :status
		AND o.retry_count < :retryCount
		ORDER BY o.id ASC
		FOR UPDATE SKIP LOCKED
		""", nativeQuery = true)
	List<AuctionOutbox> findAllByStatusAndRetryCountLessThanWithLock(
		@Param("status") AuctionOutboxStatus status,
		@Param("retryCount") int retryCount
	);
}
