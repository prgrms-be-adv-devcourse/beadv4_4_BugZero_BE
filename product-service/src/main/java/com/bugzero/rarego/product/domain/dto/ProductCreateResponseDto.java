package com.bugzero.rarego.product.domain.dto;

import com.bugzero.rarego.shared.product.type.InspectionStatus;

import lombok.Builder;

@Builder
public record ProductCreateResponseDto(
	long productId,
	InspectionStatus inspectionStatus
) {
}
