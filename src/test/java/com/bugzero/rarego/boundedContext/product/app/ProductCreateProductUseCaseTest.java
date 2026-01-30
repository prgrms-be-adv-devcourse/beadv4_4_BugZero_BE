package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;

@ExtendWith(MockitoExtension.class)
class ProductCreateProductUseCaseTest {

	@InjectMocks
	private ProductCreateProductUseCase useCase;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private AuctionApiClient auctionApiClient;

	@Mock
	private ProductSupport productSupport;

	@Mock
	private EventPublisher eventPublisher; // 수정된 의존성

	@Test
	@DisplayName("성공: 상품이 등록되면 DB에는 products/ 경로로 저장되고 S3 확정 이벤트가 발행된다")
	void createProduct_success() {
		// given
		String memberUUID = "seller-uuid";
		Long expectedAuctionId = 100L;
		String tempUrl = "temp/starwars.jpg";

		ProductCreateRequestDto request = new ProductCreateRequestDto(
			"스타워즈 시리즈",
			Category.스타워즈,
			"설명",
			new ProductAuctionRequestDto(1000, 7),
			List.of(new ProductImageRequestDto(tempUrl, 0))
		);

		ProductMember seller = ProductMember.builder().id(1L).build();

		given(productSupport.verifyValidateMember(memberUUID)).willReturn(seller);
		given(productSupport.normalizeCreateImageOrder(anyList()))
			.willReturn(request.productImageRequestDto());

		given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
			Product product = invocation.getArgument(0);
			ReflectionTestUtils.setField(product, "id", 1L);
			return product;
		});

		given(auctionApiClient.createAuction(eq(1L), eq(memberUUID), any(ProductAuctionRequestDto.class)))
			.willReturn(expectedAuctionId);

		// when
		ProductCreateResponseDto response = useCase.createProduct(memberUUID, request);

		// then
		// 1. DB 저장 경로 검증 (ProductImage.createConfirmedImage 내부에 치환 로직이 있다고 가정)
		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		verify(productRepository).save(productCaptor.capture());
		Product savedProduct = productCaptor.getValue();

		assertThat(savedProduct.getImages().get(0).getImageUrl())
			.isEqualTo("products/starwars.jpg"); // 정적 팩토리 메서드에서 처리된 결과 검증

		// 2. 이벤트 발행 검증 (핵심 변경 사항)
		ArgumentCaptor<S3ImageConfirmEvent> eventCaptor = ArgumentCaptor.forClass(S3ImageConfirmEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());

		S3ImageConfirmEvent publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent.paths()).containsExactly(tempUrl); // 원본 temp 경로가 담겼는지 확인

		// 3. 외부 API 호출 및 응답값 검증
		verify(auctionApiClient).createAuction(eq(1L), eq(memberUUID), any());
		assertThat(response.productId()).isEqualTo(1L);
		assertThat(response.auctionId()).isEqualTo(expectedAuctionId);
	}
}