package com.bugzero.rarego.bounded_context.payment.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.bugzero.rarego.bounded_context.payment.domain.Settlement;
import com.bugzero.rarego.bounded_context.payment.domain.SettlementStatus;

import jakarta.persistence.LockModeType;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Settlement s WHERE s.auctionId = :auctionId")
	Optional<Settlement> findByAuctionIdForUpdate(Long auctionId);

	List<Settlement> findAllByStatus(SettlementStatus status);

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
		AND (:to IS NULL OR s.createdAt < :to)
		""")
	Page<Settlement> searchSettlements(Long sellerId, SettlementStatus status, LocalDateTime from,
		LocalDateTime to, Pageable pageable);

	@Query("""
		SELECT s
		FROM Settlement s
		JOIN FETCH s.seller
		WHERE s.status = :status AND s.createdAt < :cutoffDate
		ORDER BY s.id ASC
		""")
	List<Settlement> findSettlementsForBatch(SettlementStatus status, LocalDateTime cutoffDate, Pageable pageable);
}
