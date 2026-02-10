package com.bugzero.rarego.in.dto.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSearchDocumentDto {

	private String id;

	@JsonProperty("productId")
	private Long productId;

	@JsonProperty("productName")
	private String productName;

	@JsonProperty("description")
	private String description;

	@JsonProperty("imageUrl")
	private String imageUrl;

	@JsonProperty("startPrice")
	private int startPrice;

	@JsonProperty("sellerId")
	private Long sellerId;

	@JsonProperty("category")
	private String category;
}