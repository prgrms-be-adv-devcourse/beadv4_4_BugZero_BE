package com.bugzero.rarego.shared.product.dto;

import java.util.ArrayList;
import java.util.List;

import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ProductRequestDto(
	@NotBlank(message = "상품명은 필수입니다.")
	String name,
	@NotNull(message = "카테고리는 필수입니다.")
	Category category,
	@NotNull(message = "상세설명은 필수입니다.")
	String description,
	@Valid
	@NotNull(message = "경매 정보는 필수입니다.")
	ProductAuctionRequestDto productAuctionRequestDto,
	@NotEmpty(message = "이미지는 최소 1장 이상 등록해야 합니다.")
	List<@Valid ProductImageRequestDto> productImageRequestDto
) {
	public Product toEntity(Long memberId) {
		return Product.builder()
			.sellerId(memberId)
			.category(category)
			.productCondition(ProductCondition.INSPECTION)
			// 처음 상품이 등록될 때는 검수 대기상태로 지정
			.inspectionStatus(InspectionStatus.PENDING)
			.images(new ArrayList<>())
			.name(name)
			.description(description)
			.build();
	}
}
