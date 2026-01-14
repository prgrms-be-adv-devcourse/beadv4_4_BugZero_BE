package com.bugzero.rarego.boundedContext.product.domain;

import java.util.List;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	private long sellerId;
	@Enumerated(EnumType.STRING)
	private Category category;
	@Enumerated(EnumType.STRING)
	private ProductCondition productCondition;
	@Enumerated(EnumType.STRING)
	private InspectionStatus inspectionStatus;
	@OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
	List<ProductImage> images;
	@Column(length = 100, nullable = false)
	private String name;
	@Column(columnDefinition = "TEXT")
	private String description;

	//TODO 추후 상품이미지 등록 로직에 따라 파라미터 값 변경 예정
	public void addImage(ProductImage image) {
		this.images.add(image);
	}
}
