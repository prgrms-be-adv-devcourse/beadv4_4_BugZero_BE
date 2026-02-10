package com.bugzero.rarego.ai.domain.dto;

import java.time.LocalDateTime;

import com.bugzero.rarego.shared.product.type.Category;
import com.bugzero.rarego.shared.product.type.ProductCondition;

public record AiInternalPriceResponseDto(
	String id,
	Long productId,
	Long auctionId,
	String productName,
	ProductCondition productCondition,
	Category category,
	int startPrice,
	int finalPrice,
	LocalDateTime startedAt,
	LocalDateTime closedAt,
	String thumbnail,
	double score
) {
}
