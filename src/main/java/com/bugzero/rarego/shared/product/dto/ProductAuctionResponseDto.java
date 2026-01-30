package com.bugzero.rarego.shared.product.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAuctionResponseDto {

	private Long id;
	private Long sellerId;
	private String name;
	private String description;
	private String category;
	private String thumbnailUrl;
	private List<String> imageUrls;

}
