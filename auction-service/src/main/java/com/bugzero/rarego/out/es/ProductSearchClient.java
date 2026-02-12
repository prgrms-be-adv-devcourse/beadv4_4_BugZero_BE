package com.bugzero.rarego.out.es;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bugzero.rarego.in.dto.es.ProductSearchDocumentDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;
import com.bugzero.rarego.shared.product.type.Category;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchClient {

	private final ElasticsearchClient elasticsearchClient;
	private static final String INDEX_NAME = "product_search";

	/**
	 * [단건 조회] 상품 ID로 상품 정보 조회
	 * (ProductApiClient.getProduct 대응)
	 */
	public Optional<ProductAuctionResponseDto> getProduct(Long productId) {
		if (productId == null)
			return Optional.empty();

		// 단건 조회도 search 쿼리를 사용 (findByProductId와 동일 효과)
		// 복합키 구조라면 term 쿼리로 productId 필드를 검색해야 함
		Map<Long, ProductAuctionResponseDto> result = getProducts(Set.of(productId));
		return Optional.ofNullable(result.get(productId));
	}

	/**
	 * [다건 조회] 상품 ID 목록으로 상품 정보 일괄 조회
	 * (ProductApiClient.getProducts 대응)
	 */
	public Map<Long, ProductAuctionResponseDto> getProducts(Collection<Long> productIds) {
		if (productIds == null || productIds.isEmpty())
			return Collections.emptyMap();

		try {
			List<FieldValue> values = productIds.stream().map(FieldValue::of).toList();

			SearchResponse<ProductSearchDocumentDto> response = elasticsearchClient.search(s -> s
					.index(INDEX_NAME)
					.size(productIds.size()) // 요청한 개수만큼 조회
					.query(q -> q.terms(t -> t.field("productId").terms(v -> v.value(values)))),
				ProductSearchDocumentDto.class
			);

			return response.hits().hits().stream()
				.map(Hit::source)
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(
					ProductSearchDocumentDto::getProductId,
					this::convertToDto,
					(existing, replacement) -> existing // 중복 시 기존 것 유지 (또는 최신 것 선택)
				));

		} catch (IOException e) {
			log.error("ES Bulk Get Error", e);
			return Collections.emptyMap();
		}
	}

	/**
	 * [판매자 상품 조회] 판매자 ID로 상품 ID 목록 조회
	 * (ProductApiClient.getProductIdsBySellerId 대응)
	 */
	public List<Long> getProductIdsBySellerId(Long sellerId) {
		if (sellerId == null)
			return Collections.emptyList();

		try {
			SearchResponse<ProductSearchDocumentDto> response = elasticsearchClient.search(s -> s
					.index(INDEX_NAME)
					.size(1000) // 최대 1000개 (필요시 조정)
					.source(src -> src.filter(f -> f.includes("productId"))) // ID만 조회 최적화
					.query(q -> q.term(t -> t.field("sellerId").value(sellerId))),
				ProductSearchDocumentDto.class
			);

			return response.hits().hits().stream()
				.map(Hit::source)
				.filter(Objects::nonNull)
				.map(ProductSearchDocumentDto::getProductId)
				.distinct()
				.toList();
		} catch (IOException e) {
			log.error("ES Seller Search Error", e);
			return Collections.emptyList();
		}
	}

	/**
	 * [검색] 키워드 및 카테고리로 상품 ID 목록 검색
	 * (ProductApiClient.searchProductIds 대응)
	 */
	public List<Long> searchProductIds(String keyword, Category category) {
		try {
			List<Query> filters = new ArrayList<>();
			if (category != null) {
				filters.add(Query.of(q -> q.term(t -> t.field("category").value(category.name()))));
			}

			SearchResponse<ProductSearchDocumentDto> response = elasticsearchClient.search(s -> {
				s.index(INDEX_NAME)
					.size(1000)
					.source(src -> src.filter(f -> f.includes("productId")));

				if (StringUtils.hasText(keyword)) {
					s.query(q -> q.bool(b -> b
						.should(sh -> sh.match(m -> m.field("productName").query(keyword).boost(2.0f)))
						.should(sh -> sh.match(m -> m.field("description").query(keyword)))
						.filter(filters)
					));
				} else {
					s.query(q -> q.bool(b -> b.filter(filters)));
				}
				return s;
			}, ProductSearchDocumentDto.class);

			return response.hits().hits().stream()
				.map(Hit::source)
				.filter(Objects::nonNull)
				.map(ProductSearchDocumentDto::getProductId)
				.distinct()
				.toList();

		} catch (IOException e) {
			log.error("ES Search Error", e);
			return Collections.emptyList();
		}
	}

	/**
	 * [승인 상품 조회] 검수 승인된 상품 ID 목록 조회
	 * (ProductApiClient.getApprovedProductIds 대응)
	 * - ES에 있는 데이터는 기본적으로 승인 후 등록된 것이라 가정
	 * - 필요하다면 status 필터를 추가할 수 있음
	 */
	public List<Long> getApprovedProductIds() {
		try {
			SearchResponse<ProductSearchDocumentDto> response = elasticsearchClient.search(s -> s
					.index(INDEX_NAME)
					.size(1000)
					.source(src -> src.filter(f -> f.includes("productId")))
					.query(q -> q.matchAll(m -> m)), // 전체 조회 (또는 특정 조건)
				ProductSearchDocumentDto.class
			);

			return response.hits().hits().stream()
				.map(Hit::source)
				.filter(Objects::nonNull)
				.map(ProductSearchDocumentDto::getProductId)
				.distinct()
				.toList();
		} catch (IOException e) {
			log.error("ES Approved List Error", e);
			return Collections.emptyList();
		}
	}

	// Helper: DTO 변환 (Builder 패턴 적용)
	private ProductAuctionResponseDto convertToDto(ProductSearchDocumentDto doc) {
		String imageUrl = doc.getImageUrl() != null ? doc.getImageUrl() : "";

		return ProductAuctionResponseDto.builder()
			.id(doc.getProductId())
			.sellerId(doc.getSellerId())
			.name(doc.getProductName())
			.description(doc.getDescription())
			.category(doc.getCategory())
			.thumbnailUrl(imageUrl)
			.imageUrls(List.of(imageUrl)) // 목록형에서는 1장만 있어도 무방
			.build();
	}
}
