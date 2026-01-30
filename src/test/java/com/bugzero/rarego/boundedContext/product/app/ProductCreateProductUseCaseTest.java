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
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;

@ExtendWith(MockitoExtension.class)
class ProductCreateProductUseCaseTest {

	@InjectMocks
	private ProductCreateProductUseCase useCase;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private AuctionApiClient auctionApiClient;

	@Mock
	private ProductImageS3UseCase productImageS3UseCase; // 추가된 의존성

	@Mock
	private ProductSupport productSupport;

	@Test
	@DisplayName("성공: 상품 정보가 등록되고, 이미지 경로가 치환되며 S3 비동기 확정 로직이 호출된다")
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

		// 1. 유효 멤버 반환
		given(productSupport.verifyValidateMember(memberUUID)).willReturn(seller);

		// 2. 이미지 정렬 로직 (받은 그대로 반환하도록 설정)
		given(productSupport.normalizeCreateImageOrder(anyList()))
			.willReturn(request.productImageRequestDto());

		// 3. Repository 저장 시 ID 주입
		given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
			Product product = invocation.getArgument(0);
			ReflectionTestUtils.setField(product, "id", 1L);
			return product;
		});

		// 4. 경매 API 호출 결과
		given(auctionApiClient.createAuction(eq(1L), eq(memberUUID), any(ProductAuctionRequestDto.class)))
			.willReturn(expectedAuctionId);

		// when
		ProductCreateResponseDto response = useCase.createProduct(memberUUID, request);

		// then
		// 1. Repository 저장 데이터 검증 (ArgumentCaptor 활용)
		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		verify(productRepository).save(productCaptor.capture());
		Product savedProduct = productCaptor.getValue();

		assertThat(savedProduct.getName()).isEqualTo("스타워즈 시리즈");
		// DB 저장 시 경로가 temp/에서 products/로 치환되었는지 검증
		assertThat(savedProduct.getImages().get(0).getImageUrl()).isEqualTo("products/starwars.jpg");

		// 2. S3 비동기 확정 로직 호출 검증 (핵심!)
		// 서비스 코드에서 tempPaths 리스트를 넘기는지 확인
		ArgumentCaptor<List<String>> tempPathsCaptor = ArgumentCaptor.forClass(List.class);
		verify(productImageS3UseCase).confirmImages(tempPathsCaptor.capture());
		assertThat(tempPathsCaptor.getValue()).containsExactly(tempUrl);

		// 3. 외부 API 호출 및 반환값 검증
		verify(auctionApiClient).createAuction(eq(1L), eq(memberUUID), any());
		assertThat(response.productId()).isEqualTo(1L);
		assertThat(response.auctionId()).isEqualTo(expectedAuctionId);
		assertThat(response.inspectionStatus()).isEqualTo(InspectionStatus.PENDING);
	}
}