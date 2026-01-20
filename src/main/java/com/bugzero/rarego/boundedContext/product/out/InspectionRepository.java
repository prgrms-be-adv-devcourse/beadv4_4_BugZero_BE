package com.bugzero.rarego.boundedContext.product.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.product.domain.Inspection;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
}
