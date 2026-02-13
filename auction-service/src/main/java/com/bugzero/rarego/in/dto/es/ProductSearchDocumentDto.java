package com.bugzero.rarego.in.dto.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductSearchDocumentDto (

	String id,

	@JsonProperty("productId")
	Long productId,

	@JsonProperty("productName")
	String productName,

	@JsonProperty("description")
	String description,

	@JsonProperty("imageUrl")
	String imageUrl,

	@JsonProperty("startPrice")
	int startPrice,

	@JsonProperty("sellerId")
	Long sellerId,

	@JsonProperty("category")
	String category

) {

}
