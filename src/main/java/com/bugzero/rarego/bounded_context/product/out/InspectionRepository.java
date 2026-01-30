package com.bugzero.rarego.bounded_context.product.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.bounded_context.product.domain.Inspection;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
}
