package com.bugzero.rarego.boundedContext.product.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.product.domain.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}
