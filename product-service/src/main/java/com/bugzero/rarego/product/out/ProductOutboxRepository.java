package com.bugzero.rarego.product.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.product.domain.ProductOutbox;

public interface ProductOutboxRepository extends JpaRepository<ProductOutbox, Long> {
}
