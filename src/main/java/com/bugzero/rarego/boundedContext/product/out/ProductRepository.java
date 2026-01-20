package com.bugzero.rarego.boundedContext.product.out;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.boundedContext.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findByIdAndDeletedIsFalse(Long id);

	// 판매자의 모든 상품(경매) ID 조회
	@Query("SELECT p.id FROM Product p WHERE p.sellerId = :sellerId")
	List<Long> findAllIdsBySellerId(@Param("sellerId") Long sellerId);

	// 상품 ID 목록으로 상품 엔티티 일괄 조회
	List<Product> findAllByIdIn(Collection<Long> ids);
}
