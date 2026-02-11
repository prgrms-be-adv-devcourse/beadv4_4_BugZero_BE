package com.bugzero.rarego.product.app;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductImage;
import com.bugzero.rarego.product.domain.document.ProductSearchDocument;
import com.bugzero.rarego.product.out.ProductSearchRepository;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.product.type.Category;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

	private final ProductSearchRepository searchRepository;
	private final ElasticsearchClient elasticsearchClient;
	@Qualifier("openAiEmbeddingModel") private final EmbeddingModel embeddingModel;

	// 초기 데이터 적재
	@Transactional
	public void save(
		Product product,
		List<ProductImage> images,
		Long auctionId,
		int startPrice,
		LocalDateTime startedAt
	) {

		String docId = ProductSearchDocument.generateId(product.getId(), auctionId);

		// 임베딩 생성
		String textToEmbed = String.format(
			ProductSearchDocument.EMBEDDING_TEMPLATE,
			product.getName(),
			product.getDescription(),
			product.getCategory(),
			product.getProductCondition()
		);

		List<Float> vector = generateEmbeddingToFloat(textToEmbed);

		String imageUrl = images.stream()
			.sorted(Comparator.comparingInt(ProductImage::getSortOrder))
			.map(ProductImage::getImageUrl)
			.findFirst()
			.orElse(null);

		ProductSearchDocument doc = ProductSearchDocument.builder()
			.id(docId)
			.productId(product.getId())
			.productName(product.getName())
			.description(product.getDescription())
			.productCondition(product.getProductCondition())
			.category(product.getCategory())
			.sellerId(product.getSeller().getId())
			.imageUrl(imageUrl)
			.embedding(vector)

			// 경매 정보 매핑
			.auctionId(auctionId)
			.startPrice(startPrice)
			.startedAt(startedAt)
			.auctionStatus(AuctionStatus.SCHEDULED)
			.finalPrice(0)
			.closedAt(null)
			.build();

		searchRepository.save(doc);
		log.info("Product Indexed (APPROVED): productId={}, auctionId={}", product.getId(), auctionId);
	}

	public void delete(Long productId) {
		searchRepository.deleteByProductId(productId);
		log.info("All Product Documents Deleted: productId={}", productId);
	}

	// 낙찰 시 가격 상태 업데이트
	public void updateSoldPrice(Long productId, Long auctionId, int finalPrice) {

		String docId = ProductSearchDocument.generateId(productId, auctionId);

		ProductSearchDocument doc = searchRepository.findById(docId)
			.orElseThrow(() -> new RuntimeException("ES Document Not Found: " + productId));

		ProductSearchDocument updatedDoc = ProductSearchDocument.builder()
			.id(doc.getId())
			.productId(doc.getProductId())
			.auctionId(doc.getAuctionId())
			.productName(doc.getProductName())
			.description(doc.getDescription())
			.productCondition(doc.getProductCondition())
			.category(doc.getCategory())
			.auctionStatus(AuctionStatus.ENDED)
			.startPrice(doc.getStartPrice())
			.finalPrice(finalPrice)
			.startedAt(doc.getStartedAt())
			.closedAt(LocalDateTime.now())
			.sellerId(doc.getSellerId())
			.imageUrl(doc.getImageUrl())
			.embedding(doc.getEmbedding())
			.build();

		searchRepository.save(updatedDoc);
		log.info("Product Sold Updated: {} -> {} won", productId, finalPrice);
	}

	// 경매 시작, 종료 등 상태만 변경할 때 사용
	public void updateAuctionStatus(Long productId, Long auctionId, AuctionStatus newStatus) {
		String docId = ProductSearchDocument.generateId(productId, auctionId);
		searchRepository.findById(docId).ifPresent(doc -> {
			ProductSearchDocument updated = ProductSearchDocument.builder()
				.id(doc.getId())
				.productId(doc.getProductId())
				.auctionId(doc.getAuctionId())
				.productName(doc.getProductName())
				.description(doc.getDescription())
				.productCondition(doc.getProductCondition())
				.category(doc.getCategory())
				.sellerId(doc.getSellerId())
				.imageUrl(doc.getImageUrl())
				.embedding(doc.getEmbedding())
				.startPrice(doc.getStartPrice())
				.finalPrice(doc.getFinalPrice())
				.startedAt(doc.getStartedAt())
				.closedAt(doc.getClosedAt())
				// 새로운 상태 적용
				.auctionStatus(newStatus)
				.build();

			searchRepository.save(updated);
			log.info("Auction Status Updated: productId={}, status={}", productId, newStatus);
		});
	}


	// 키워드(BM25) + 벡터(KNN) + 필터링
	// 텍스트 매칭 + 벡터 유사도 검색 + 카테고리/상태 필터링
	public Page<ProductSearchDocument> searchProducts(
		String keyword,
		Category category,
		AuctionStatus auctionStatus,
		Pageable pageable
	) {
		try {
			// 공통 필터 생성 (Query 객체 리스트)
			List<Query> filters = new ArrayList<>();
			if (category != null) {
				filters.add(Query.of(q -> q.term(t -> t.field("category").value(category.name()))));
			}
			if (auctionStatus != null) {
				filters.add(Query.of(q -> q.term(t -> t.field("auctionStatus").value(auctionStatus.name()))));
			}

			// 검색 요청 빌드 (ElasticsearchClient 사용)
			SearchResponse<ProductSearchDocument> response = elasticsearchClient.search(s -> {
				// 기본 설정: 인덱스, 페이징
				s.index("product_search")
					.from((int) pageable.getOffset())
					.size(pageable.getPageSize());

				// 텍스트 쿼리 구성
				if (StringUtils.hasText(keyword)) {
					s.query(q -> q.bool(b -> b
						.should(sh -> sh.match(m -> m.field("productName").query(keyword).boost(0.7f)))
						.should(sh -> sh.match(m -> m.field("description").query(keyword).boost(0.5f)))
						.filter(filters) // 공통 필터 적용
					));

					// 벡터(KNN) 쿼리 구성
					String queryText = buildSearchPrompt(keyword, category);
					List<Float> queryVector = generateEmbeddingToFloat(queryText);

					s.knn(k -> k
						.field("embedding")
						.queryVector(queryVector)
						.k(10)
						.numCandidates(100)
						.filter(f -> f.bool(b -> b.filter(filters)))
						.boost(1.5f) // 벡터 점수 비중
					);
				} else {
					// 키워드 없을 땐 필터만 적용
					s.query(q -> q.bool(b -> b.filter(filters)));
				}

				return s;
			}, ProductSearchDocument.class);

			// 결과 변환
			List<ProductSearchDocument> content = response.hits().hits().stream()
				.map(Hit::source)
				.toList();

			// total hits 계산
			long total = response.hits().total() != null ? response.hits().total().value() : 0;

			return new PageImpl<>(content, pageable, total);

		} catch (IOException e) {
			log.error("Elasticsearch Search Error", e);
			throw new RuntimeException("Search failed", e);
		}
	}

	// [Helper Method] //

	// 임베딩 생성 (Float 리스트로 변환)
	private List<Float> generateEmbeddingToFloat(String text) {
		float[] embeddingArray = embeddingModel.embed(text);
		List<Float> floatList = new ArrayList<>();
		for (float v : embeddingArray) {
			floatList.add(v);
		}
		return floatList;
	}

	// 문자열 포맷으로 변환
	private String buildSearchPrompt(String keyword, Category category) {
		return String.format(ProductSearchDocument.EMBEDDING_TEMPLATE,
			keyword, keyword,
			(category != null ? category.name() : ""),
			""
		);
	}
}