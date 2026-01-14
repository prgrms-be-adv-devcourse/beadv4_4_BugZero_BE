package com.bugzero.rarego.boundedContext.product.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
