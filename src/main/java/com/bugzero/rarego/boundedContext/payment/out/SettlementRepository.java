package com.bugzero.rarego.boundedContext.payment.out;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findAllByStatus(SettlementStatus status);
}
