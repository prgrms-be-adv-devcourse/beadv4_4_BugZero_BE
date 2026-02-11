package com.bugzero.rarego.product.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.embedding.EmbeddingModel;

import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductImage;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.domain.document.ProductSearchDocument;
import com.bugzero.rarego.product.out.ProductSearchRepository;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.product.type.Category;
import com.bugzero.rarego.shared.product.type.ProductCondition;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductSearchServiceTest {

	@Mock
	private ProductSearchRepository searchRepository;

	@Mock
	private ElasticsearchClient elasticsearchClient;

	@Mock
	private EmbeddingModel embeddingModel;

	@InjectMocks
	private ProductSearchService productSearchService;

	private static final float[] DUMMY_VECTOR = new float[1536];

	@BeforeEach
	void setUp() {
		given(embeddingModel.embed(anyString())).willReturn(DUMMY_VECTOR);
	}

	@Test
	@DisplayName("상품을 ES에 적재하면 복합 ID(pid_aid)와 SCHEDULED 상태로 저장된다")
	void save_shouldSaveProductWithCompositeIdAndStatus() {
		// given
		Long productId = 1L;
		Long auctionId = 100L;
		String productName = "다스베이더 레고";
		String expectedDocId = productId + "_" + auctionId; // 예상되는 복합 ID

		int startPrice = 1000000;
		LocalDateTime startedAt = LocalDateTime.now().minusHours(1);

		Product mockProduct = createMockProduct(productId, productName, "설명", Category.STARWARS);
		ProductImage mockImage = createMockImage("http://image.url", 0);

		// when
		productSearchService.save(mockProduct, List.of(mockImage), auctionId, startPrice, startedAt);

		// then
		ArgumentCaptor<ProductSearchDocument> captor = ArgumentCaptor.forClass(ProductSearchDocument.class);
		verify(searchRepository).save(captor.capture());

		ProductSearchDocument savedDoc = captor.getValue();

		// [검증] ID가 "1_100" 형태로 생성되었는지 확인
		assertThat(savedDoc.getId()).isEqualTo(expectedDocId);
		assertThat(savedDoc.getProductId()).isEqualTo(productId);
		assertThat(savedDoc.getAuctionId()).isEqualTo(auctionId);
		assertThat(savedDoc.getAuctionStatus()).isEqualTo(AuctionStatus.SCHEDULED);
		assertThat(savedDoc.getImageUrl()).isEqualTo("http://image.url");
	}

	@Test
	@DisplayName("여러 이미지 중 sortOrder가 가장 낮은 이미지가 대표 이미지로 저장된다")
	void save_shouldSelectFirstImageBySortOrder() {
		// given
		Product mockProduct = createMockProduct(2L, "테스트 상품", "설명", Category.STARWARS);
		ProductImage image1 = createMockImage("http://second.jpg", 1);
		ProductImage image2 = createMockImage("http://first.jpg", 0);
		ProductImage image3 = createMockImage("http://third.jpg", 2);

		// when
		productSearchService.save(mockProduct, List.of(image1, image2, image3), 101L, 500000, LocalDateTime.now());

		// then
		ArgumentCaptor<ProductSearchDocument> captor = ArgumentCaptor.forClass(ProductSearchDocument.class);
		verify(searchRepository).save(captor.capture());

		assertThat(captor.getValue().getImageUrl()).isEqualTo("http://first.jpg");
	}

	@Test
	@DisplayName("상품 삭제 시 ES에서 해당 상품의 모든 문서가 제거된다 (deleteByProductId 호출)")
	void delete_shouldCallDeleteByProductId() {
		// given
		Long productId = 3L;

		// when
		productSearchService.delete(productId);

		// then
		// [변경] delete(entity)가 아니라 deleteByProductId(id)가 호출되어야 함
		verify(searchRepository).deleteByProductId(productId);
	}

	@Test
	@DisplayName("낙찰 시 해당 경매(auctionId)를 찾아 최종 가격과 ENDED 상태로 업데이트한다")
	void updateSoldPrice_shouldUpdateFinalPriceAndStatus() {
		// given
		Long productId = 4L;
		Long auctionId = 200L;
		int finalPrice = 750000;
		String docId = productId + "_" + auctionId; // 복합 키

		ProductSearchDocument existingDoc = ProductSearchDocument.builder()
			.id(docId)
			.productId(productId)
			.auctionId(auctionId)
			.productName("테스트 상품")
			.auctionStatus(AuctionStatus.IN_PROGRESS)
			.startPrice(500000)
			.finalPrice(0)
			.build();

		// [변경] findByProductId 대신 findById(복합키) Mocking
		given(searchRepository.findById(docId)).willReturn(Optional.of(existingDoc));

		// when
		// [변경] auctionId 파라미터 추가
		productSearchService.updateSoldPrice(productId, auctionId, finalPrice);

		// then
		ArgumentCaptor<ProductSearchDocument> captor = ArgumentCaptor.forClass(ProductSearchDocument.class);
		verify(searchRepository).save(captor.capture());

		ProductSearchDocument updatedDoc = captor.getValue();
		assertThat(updatedDoc.getId()).isEqualTo(docId); // ID 유지 확인
		assertThat(updatedDoc.getFinalPrice()).isEqualTo(finalPrice);
		assertThat(updatedDoc.getAuctionStatus()).isEqualTo(AuctionStatus.ENDED);
		assertThat(updatedDoc.getClosedAt()).isNotNull();
	}

	@Test
	@DisplayName("특정 경매의 상태를 변경할 수 있다")
	void updateAuctionStatus_shouldUpdateStatus() {
		// given
		Long productId = 5L;
		Long auctionId = 300L;
		String docId = productId + "_" + auctionId;

		ProductSearchDocument existingDoc = ProductSearchDocument.builder()
			.id(docId)
			.productId(productId)
			.auctionId(auctionId)
			.auctionStatus(AuctionStatus.SCHEDULED)
			.build();

		// [변경] findById Mocking
		given(searchRepository.findById(docId)).willReturn(Optional.of(existingDoc));

		// when
		// [변경] auctionId 파라미터 추가
		productSearchService.updateAuctionStatus(productId, auctionId, AuctionStatus.IN_PROGRESS);

		// then
		ArgumentCaptor<ProductSearchDocument> captor = ArgumentCaptor.forClass(ProductSearchDocument.class);
		verify(searchRepository).save(captor.capture());

		assertThat(captor.getValue().getAuctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
	}

	// === Helper Methods ===

	private Product createMockProduct(Long id, String name, String description, Category category) {
		Product mockProduct = mock(Product.class);
		ProductMember mockSeller = mock(ProductMember.class);

		when(mockProduct.getId()).thenReturn(id);
		when(mockProduct.getName()).thenReturn(name);
		when(mockProduct.getDescription()).thenReturn(description);
		when(mockProduct.getCategory()).thenReturn(category);
		when(mockProduct.getProductCondition()).thenReturn(ProductCondition.MISB);
		when(mockProduct.getSeller()).thenReturn(mockSeller);
		when(mockSeller.getId()).thenReturn(999L);

		return mockProduct;
	}

	private ProductImage createMockImage(String url, int sortOrder) {
		ProductImage mockImage = mock(ProductImage.class);
		when(mockImage.getImageUrl()).thenReturn(url);
		when(mockImage.getSortOrder()).thenReturn(sortOrder);
		return mockImage;
	}
}