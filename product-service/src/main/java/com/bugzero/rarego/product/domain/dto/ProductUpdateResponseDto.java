package com.bugzero.rarego.product.domain.dto;

import lombok.Builder;

@Builder
public record ProductUpdateResponseDto (
	Long productId
){
}
