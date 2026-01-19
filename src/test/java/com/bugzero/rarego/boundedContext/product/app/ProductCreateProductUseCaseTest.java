package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.product.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;

@ExtendWith(MockitoExtension.class)
class ProductCreateProductUseCaseTest {
	@InjectMocks
	private ProductCreateProductUseCase useCase;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private AuctionApiClient auctionApiClient;

	@Test
	@DisplayName("상품 정보 등록 성공")
	void createProduct_success() {
		// given
		String memberId = "1L";
		Long expectedAuctionId = 100L;

		ProductRequestDto request = new ProductRequestDto(
			"스타워즈 시리즈",
			Category.스타워즈,
			"설명",
			new ProductAuctionRequestDto(1000, 7),
			List.of(new ProductImageRequestDto("url", 0))
		);

		//productRepository 에 저장되었을 때 반환되는 값 지정
		given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
			Product product = invocation.getArgument(0); //들어온 인자값
			ReflectionTestUtils.setField(product, "id", 1L); // 임의로 ID 부여
			return product;
		});

		given(auctionApiClient.createAuction(eq(1L),eq("1L"), any(ProductAuctionRequestDto.class)))
			.willReturn(expectedAuctionId);

		// when
		ProductResponseDto response = useCase.createProduct(memberId, request);
		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

		verify(productRepository, times(1)).save(productCaptor.capture());
		verify(auctionApiClient, times(1)).createAuction(eq(1L),eq("1L"), any());

		Product product = productCaptor.getValue();

		// then
		// 저장값 검증
		assertThat(product.getName()).isEqualTo("스타워즈 시리즈");
		assertThat(product.getCategory()).isEqualTo(Category.스타워즈);
		assertThat(product.getDescription()).isEqualTo("설명");
		assertThat(product.getImages().size()).isEqualTo(1);

		// 반환값 검증
		assertThat(response.productId()).isNotNull();
		assertThat(response.auctionId()).isNotNull();
		assertThat(response.auctionId()).isEqualTo(expectedAuctionId);
		assertThat(response.inspectionStatus()).isEqualTo(InspectionStatus.PENDING);
	}
}