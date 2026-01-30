package com.bugzero.rarego.boundedContext.product.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.dto.ProductImageUpdateDto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "PRODUCT_PRODUCT")
@Builder
public class Product extends BaseIdAndTime {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_id",  nullable = false)
	private ProductMember seller;
	@Enumerated(EnumType.STRING)
	private Category category;
	@Enumerated(EnumType.STRING)
	private ProductCondition productCondition;
	@Enumerated(EnumType.STRING)
	private InspectionStatus inspectionStatus;
	@Builder.Default //images 가 null 값을 가지는 것을 방지
	@OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
	List<ProductImage> images =  new ArrayList<>();
	@Column(length = 100, nullable = false)
	private String name;
	@Column(columnDefinition = "TEXT")
	private String description;

	public void addImage(ProductImage image) {
		this.images.add(image);
	}

	public void determineInspection(InspectionStatus inspectionStatus) {
		this.inspectionStatus = inspectionStatus;
	}

	public void determineProductCondition(ProductCondition productCondition) {
		this.productCondition = productCondition;
	}

	//해당 상품이 로그인한 회원의 것이 맞는지 확인
	public boolean isSeller(Long memberId) {
		return Objects.equals(this.seller.getId(), memberId);
	}

	public boolean isPending() {
		return this.inspectionStatus == InspectionStatus.PENDING;
	}

	//검수가 이미 승인되었는지 확인
	public boolean isApproved() {
		return this.inspectionStatus == InspectionStatus.APPROVED;
	}

	public void updateBasicInfo(String name, Category category, String description) {
		this.name = name;
		this.category = category;
		this.description = description;
	}

	//삭제될 이미지 리스트 반환
	public List<String> removeOldImages(List<ProductImageUpdateDto> imageDtos) {
		Set<Long> updateIds = imageDtos.stream()
			.map(ProductImageUpdateDto::id)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		// 삭제될 이미지 필터링
		List<ProductImage> toDelete = this.images.stream()
			.filter(img -> !updateIds.contains(img.getId()))
			.toList();

		List<String> pathsToDelete = toDelete.stream()
			.map(ProductImage::getImageUrl)
			.toList();

		// 고아 객체 제거 (DB 삭제 트리거)
		this.images.removeAll(toDelete);

		return pathsToDelete;
	}

	//수정시 새롭게 등록될 이미지 리스트 반환
	public List<String> processNewImages(List<ProductImageUpdateDto> imageDtos) {
		List<String> newImages = new ArrayList<>();
		// 기존 이미지들을 Map으로 만들어 효율적으로 찾기
		Map<Long, ProductImage> currentImageMap = this.images.stream()
			.collect(Collectors.toMap(ProductImage::getId, img -> img));

		for (ProductImageUpdateDto dto : imageDtos) {
			if (dto.id() == null) {
				this.addImage(ProductImage.createConfirmedImage(this, dto.imgUrl(), dto.sortOrder()));
				newImages.add(dto.imgUrl());
			} else {
				ProductImage existingImage = Optional.ofNullable(currentImageMap.get(dto.id()))
					.orElseThrow(() -> new CustomException(ErrorType.IMAGE_NOT_FOUND));

				existingImage.update(dto.imgUrl(), dto.sortOrder());
			}
		}
		return newImages;
	}
}
