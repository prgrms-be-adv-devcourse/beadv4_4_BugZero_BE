package com.bugzero.rarego.boundedContext.product.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
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
	private String name;
	private String description;
}
