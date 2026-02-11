package com.bugzero.rarego.ai.app;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.ai.domain.dto.AiInternalPriceRequestDto;
import com.bugzero.rarego.ai.domain.dto.AiInternalPriceResponseDto;
import com.bugzero.rarego.ai.domain.type.TemporaryCondition;
import com.bugzero.rarego.product.domain.document.ProductSearchDocument;
import com.bugzero.rarego.shared.product.type.Category;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiGetInternalPriceUseCase {

	private final ElasticsearchOperations elasticsearchOperations;
	private final EmbeddingModel embeddingModel;
	//가져올 유사상품 최대갯수
	private static final int LIST_LIMIT = 3;


	public List<AiInternalPriceResponseDto> findTopSimilarProducts(
		AiInternalPriceRequestDto dto) {
		String searchQuery = String.format(ProductSearchDocument.EMBEDDING_TEMPLATE,
			dto.name(), dto.description(), dto.category(), dto.condition());

		NativeQuery nativeQuery = buildNativeQuery(searchQuery, dto.category(), dto.condition());

		// 검색 실행 및 결과 매핑
		SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(nativeQuery,
			ProductSearchDocument.class);

		return hits.getSearchHits().stream()
			.map(hit -> {
				ProductSearchDocument doc = hit.getContent();
				return new AiInternalPriceResponseDto(
					doc.getId(),
					doc.getProductId(),
					doc.getAuctionId(),
					doc.getProductName(),
					doc.getProductCondition(),
					doc.getCategory(),
					doc.getStartPrice(),
					doc.getFinalPrice(),
					doc.getStartedAt(),
					doc.getClosedAt(),
					doc.getImageUrl(),
					hit.getScore() // ES에서 계산된 유사도 점수
				);
			})
			.collect(Collectors.toList());
	}

	private NativeQuery buildNativeQuery(String query, Category category, TemporaryCondition condition) {

		// 사용자 입력 텍스트를 벡터로 변환
		List<Float> vectorList = generateEmbeddingToFloat(query);

		return NativeQuery.builder()
			.withKnnSearches(KnnSearch.of(ks -> ks
				.field("embedding")
				.queryVector(vectorList)
				.k(LIST_LIMIT)
				.numCandidates(100)
				.similarity(0.8f)
				.filter(f -> f.bool(b -> b
					.filter(ft -> ft.term(t -> t.field("category").value(category.name())))
					.filter(ft -> ft.term(t -> t.field("productCondition").value(condition.name())))
					.filter(ft -> ft.range(r -> r
						.untyped(u -> u
							.field("finalPrice")
							.gt(JsonData.of(0))
						)
					))
				))
			))
			.build();
	}

	private List<Float> generateEmbeddingToFloat(String text) {
		float[] embeddingArray = embeddingModel.embed(text);
		List<Float> floatList = new ArrayList<>();
		for (float v : embeddingArray) {
			floatList.add(v);
		}
		return floatList;
	}
}
