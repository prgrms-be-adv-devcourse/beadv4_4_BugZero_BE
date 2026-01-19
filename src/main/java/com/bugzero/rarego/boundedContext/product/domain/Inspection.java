package com.bugzero.rarego.boundedContext.product.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "PRODUCT_INSPECTION")
@Builder
public class Inspection extends BaseIdAndTime {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product	product;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_id", nullable = false)
	private ProductMember seller;
	private Long inspectorId;
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private InspectionStatus inspectionStatus;
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ProductCondition productCondition;
	@Column(columnDefinition = "TEXT")
	private String reason;

}
