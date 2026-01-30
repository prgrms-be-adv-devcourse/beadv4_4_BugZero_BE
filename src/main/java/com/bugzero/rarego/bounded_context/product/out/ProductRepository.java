package com.bugzero.rarego.bounded_context.product.out;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.bounded_context.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findByIdAndDeletedIsFalse(Long id);

	// 판매자의 모든 상품(경매) ID 조회
	@Query("SELECT p.id FROM Product p WHERE p.sellerId = :sellerId")
	List<Long> findAllIdsBySellerId(@Param("sellerId") Long sellerId);

	// 상품 ID 목록으로 상품 엔티티 일괄 조회
	List<Product> findAllByIdIn(Collection<Long> ids);

	// 동적 쿼리 또는 COALESCE를 사용한 검색 조건 구현
	// keyword가 null/빈문자열이면 무시, category가 null이면 무시
	@Query("""
        SELECT p.id FROM Product p
        WHERE (:keyword IS NULL OR :keyword = '' OR p.name LIKE %:keyword%)
        AND (:category IS NULL OR :category = '' OR p.category = :category)
    """)
	List<Long> findIdsBySearchCondition(
		@Param("keyword") String keyword,
		@Param("category") String category
	);
}
