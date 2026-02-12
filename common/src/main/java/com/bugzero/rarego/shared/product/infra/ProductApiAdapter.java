package com.bugzero.rarego.shared.product.infra;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;
import com.bugzero.rarego.shared.product.out.ProductApiClient;
import com.bugzero.rarego.shared.product.type.Category;

@Component
public class ProductApiAdapter implements ProductApiClient {

	@Override
	public Optional<ProductAuctionResponseDto> getProduct(Long productId) {
		// TODO: 나중에 실제 Product-Service API를 호출하도록 수정해야 함 (FeignClient 등 사용)
		// 현재는 에러 방지용으로 빈 값을 반환
		return Optional.empty();
	}

	@Override
	public List<ProductAuctionResponseDto> getProducts(Set<Long> productIds) {
		return Collections.emptyList();
	}

	@Override
	public List<Long> getProductIdsBySellerId(Long sellerId) {
		return Collections.emptyList();
	}

	@Override
	public List<Long> searchProductIds(String keyword, Category category) {
		return List.of();
	}

	//    @Override
	//    public List<Long> searchProductIds(String keyword, String category) {
	//        return Collections.emptyList();
	//    }

	@Override
	public List<Long> getApprovedProductIds() {
		return Collections.emptyList();
	}
}
