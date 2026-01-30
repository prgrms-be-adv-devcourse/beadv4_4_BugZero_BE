package com.bugzero.rarego.boundedContext.product.app;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.out.InspectionRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductImageRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;
import com.bugzero.rarego.shared.product.out.ProductApiClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductApiAdapter implements ProductApiClient {

	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final ProductCreateS3PresignerUrlUseCase s3PresignerUrlUseCase;
	private final InspectionRepository inspectionRepository;

	@Override
	public Optional<ProductAuctionResponseDto> getProduct(Long productId) {
		return productRepository.findById(productId)
			.map(this::convertToDto);
	}

	@Override
	public List<ProductAuctionResponseDto> getProducts(Set<Long> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Collections.emptyList();
		}
		return productRepository.findAllById(productIds).stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}

	@Override
	public List<Long> getProductIdsBySellerId(Long sellerId) {
		return productRepository.findAllIdsBySellerId(sellerId);
	}

	@Override
	public List<Long> searchProductIds(String keyword, String category) {
		return productRepository.findIdsBySearchCondition(keyword, category);
	}

	@Override
	public List<Long> getApprovedProductIds() {
		return inspectionRepository.findProductIdsByInspectionStatus(InspectionStatus.APPROVED);
	}

	// Entity -> Shared DTO 변환 메서드
	private ProductAuctionResponseDto convertToDto(Product product) {
		List<ProductImage> images = productImageRepository.findAllByProductId(product.getId());

		// 이미지 정렬 및 URL Presigning
		List<String> signedImageUrls = images.stream()
			.sorted(Comparator.comparingInt(ProductImage::getSortOrder))
			.map(img -> s3PresignerUrlUseCase.getPresignedGetUrl(img.getImageUrl()))
			.collect(Collectors.toList());

		String thumbnail = signedImageUrls.isEmpty() ? null : signedImageUrls.get(0);

		return ProductAuctionResponseDto.builder()
			.id(product.getId())
			.sellerId(product.getSeller().getId())
			.name(product.getName())
			.description(product.getDescription())
			.category(product.getCategory() != null ? product.getCategory().name() : null)
			.thumbnailUrl(thumbnail)
			.imageUrls(signedImageUrls)
			.build();
	}
}