package com.bugzero.rarego.boundedContext.product.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.shared.product.dto.ProductResponseForInspectionDto;

public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findByIdAndDeletedIsFalse(Long id);

	// 판매자의 모든 상품(경매) ID 조회
	@Query("SELECT p.id FROM Product p WHERE p.seller.id = :sellerId")
	List<Long> findAllIdsBySellerId(@Param("sellerId") Long sellerId);

	//상품 조회 시 이미지를 한번에 가져와야 할때
	@Query("select p from Product p join fetch p.images where p.id = :productId")
	Optional<Product> findByIdWithImages(@Param("productId") Long productId);

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

	@Query("""
		SELECT new com.bugzero.rarego.shared.product.dto.ProductResponseForInspectionDto(
		    p.id, 
		    p.name, 
		    s.email, 
		    p.category, 
		    p.inspectionStatus, 
		    img.imageUrl
		)
		FROM Product p
		JOIN p.seller s
		LEFT JOIN p.images img ON img.product = p AND img.sortOrder = 0
		WHERE (:name IS NULL OR p.name LIKE %:name%)
		  AND (:category IS NULL OR p.category = :category)
		  AND (:status IS NULL OR p.inspectionStatus = :status)
		""")
	Page<ProductResponseForInspectionDto> readProductsForAdmin(
		@Param("name") String name,
		@Param("category") Category category,
		@Param("status") InspectionStatus status,
		Pageable pageable
	);
}
