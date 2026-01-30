package com.bugzero.rarego.shared.product.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;

public interface ProductApiClient {
	// 상품 ID로 단건 조회 (존재하지 않을 수 있음)
	Optional<ProductAuctionResponseDto> getProduct(Long productId);

	// 상품 ID 목록으로 일괄 조회 (Bulk)
	List<ProductAuctionResponseDto> getProducts(Set<Long> productIds);

	// 판매자 ID로 해당 판매자의 상품 ID 목록 조회
	List<Long> getProductIdsBySellerId(Long sellerId);

	// 키워드 및 카테고리로 상품 ID 검색
	List<Long> searchProductIds(String keyword, String category);

	// 검수 승인된 상품 ID 목록 조회
	List<Long> getApprovedProductIds();
}
