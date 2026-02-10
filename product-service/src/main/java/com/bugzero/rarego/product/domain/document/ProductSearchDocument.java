package com.bugzero.rarego.product.domain.document;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.product.type.Category;
import com.bugzero.rarego.shared.product.type.ProductCondition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Document(indexName = "product_search")
@Setting(settingPath = "elasticsearch-settings.json")
public class ProductSearchDocument {

	@Id
	private String id;

	@Field(type = FieldType.Long)
	private Long productId; // 원본 참조

	@Field(type = FieldType.Long)
	// 경매 이력 추적이 중요한가?
	private Long auctionId; // 원본 참조

	@Field(type = FieldType.Text, analyzer = "nori")
	private String productName;

	@Field(type = FieldType.Text, analyzer = "nori")
	private String description;

	@Field(type = FieldType.Keyword)
	private ProductCondition productCondition;

	@Field(type = FieldType.Keyword)
	private Category category;

	@Field(type = FieldType.Keyword)
	private AuctionStatus auctionStatus;

	@Field(type = FieldType.Integer)
	private int startPrice;

	@Field(type = FieldType.Integer)
	private int finalPrice;

	@Field(type = FieldType.Date)
	private LocalDateTime startedAt;

	@Field(type = FieldType.Date)
	private LocalDateTime closedAt;

	// 경매 쪽 상품 정보 통신 위해서 필요
	@Field(type = FieldType.Long)
	private Long sellerId;

	@Field(type = FieldType.Text)
	private String imageUrl;

	// 상품명+설명 벡터
	@Field(type = FieldType.Dense_Vector,
		dims = 1536,
		similarity = "cosine",
		index = true)
	private List<Float> embedding;

	public static String generateId(Long productId, Long auctionId) {
		return productId + "_" + auctionId;
	}

	public static final String EMBEDDING_TEMPLATE = "상품명: %s, 상세내용: %s, 카테고리: %s, 상품상태: %s";
}

