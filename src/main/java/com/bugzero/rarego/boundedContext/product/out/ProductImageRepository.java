package com.bugzero.rarego.boundedContext.product.out;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.product.domain.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
	List<ProductImage> findAllByProductId(Long productId);

	List<ProductImage> findAllByProductIdIn(Set<Long> productIds);
}
