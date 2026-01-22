package com.bugzero.rarego.boundedContext.product.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
	//TODO 복제 멤버 엔티티 생성 후 변경
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

	//TODO 추후 상품이미지 등록 로직에 따라 파라미터 값 변경 예정
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

	public void update(String name, Category category, String description,
		List<ProductImageUpdateDto> imageDtos) {
		this.name = name;
		this.category = category;
		this.description = description;
		updateImages(imageDtos);
	}

	private void updateImages(List<ProductImageUpdateDto> imageDtos) {
		// 1. 삭제 대상 처리: 요청 DTO에 포함되지 않은 기존 이미지 ID들을 찾아 리스트에서 제거
		Set<Long> updateIds = imageDtos.stream()
			.map(ProductImageUpdateDto::id)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		this.images.removeIf(img -> !updateIds.contains(img.getId()));

		// 2. 신규 추가 및 기존 수정
		for (ProductImageUpdateDto dto : imageDtos) {
			if (dto.id() == null) {
				//ID가 없으면 완전히 새로운 이미지 추가
				this.addImage(dto.toEntity(this));
			} else {
				// 해당 ID를 가진 이미지가 현재 상품의 이미지 리스트에 있는지 확인
				//ID가 있으면 기존 이미지 -> url, 순서만 변경
				ProductImage existingImage = this.images.stream()
					.filter(img -> img.getId().equals(dto.id()))
					.findFirst()
					.orElseThrow(() -> new CustomException(ErrorType.IMAGE_NOT_FOUND));

				existingImage.update(dto.imgUrl(), dto.sortOrder());
			}
		}
	}
}
