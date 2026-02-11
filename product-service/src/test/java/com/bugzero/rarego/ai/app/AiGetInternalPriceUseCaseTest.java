package com.bugzero.rarego.ai.app;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;

import com.bugzero.rarego.ai.domain.dto.AiInternalPriceRequestDto;
import com.bugzero.rarego.ai.domain.dto.AiInternalPriceResponseDto;
import com.bugzero.rarego.ai.domain.type.TemporaryCondition;
import com.bugzero.rarego.product.domain.document.ProductSearchDocument;
import com.bugzero.rarego.shared.product.type.Category;

@ExtendWith(MockitoExtension.class)
class AiGetInternalPriceUseCaseTest {
	@Mock
	private ElasticsearchOperations elasticsearchOperations;

	@Mock
	private EmbeddingModel embeddingModel;

	@InjectMocks
	private AiGetInternalPriceUseCase useCase;

	@Test
	@DisplayName("유사 상품 검색 시 결과가 DTO로 정상 변환되어야 한다")
	void findTopSimilarProducts_Success() {
		// given
		String name = "레고 밀레니엄 팔콘";
		String description = "미개봉 새상품입니다.";
		Category category = Category.STARWARS;
		TemporaryCondition condition = TemporaryCondition.MISB;
		float[] mockVector = new float[1536]; // 가짜 벡터 배열
		AiInternalPriceRequestDto dto = new AiInternalPriceRequestDto(category, condition, name, description);

		// Mock 설정: 임베딩 모델이 가짜 벡터를 반환하도록 함
		when(embeddingModel.embed(anyString())).thenReturn(mockVector);

		// Mock 설정: ES 검색 결과 가짜 데이터 생성
		ProductSearchDocument mockDoc = ProductSearchDocument.builder()
			.id("1")
			.productId(100L)
			.productName("테스트 레고")
			.finalPrice(500000)
			.build();

		SearchHit<ProductSearchDocument> hit = new SearchHit<>(
			null, null, null, 0.95f, null, null, null, null, null, null, mockDoc);
		SearchHits<ProductSearchDocument> searchHits = new SearchHitsImpl<>(
			1, TotalHitsRelation.EQUAL_TO, 0.95f, null, null, null, List.of(hit), null, null, null);

		when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductSearchDocument.class)))
			.thenReturn(searchHits);

		// when
		List<AiInternalPriceResponseDto> result = useCase.findTopSimilarProducts(dto);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).productName()).isEqualTo("테스트 레고");
		assertThat(result.get(0).score()).isEqualTo(0.95f);

		// 검증: 각 의존성이 정확히 한 번씩 호출되었는지 확인
		verify(embeddingModel, times(1)).embed(anyString());
		verify(elasticsearchOperations, times(1)).search(any(NativeQuery.class), eq(ProductSearchDocument.class));
	}
}