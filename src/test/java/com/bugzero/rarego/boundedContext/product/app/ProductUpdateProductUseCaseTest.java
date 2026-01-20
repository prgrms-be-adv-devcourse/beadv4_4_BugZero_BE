package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductImageUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateResponseDto;

@ExtendWith(MockitoExtension.class)
class ProductUpdateProductUseCaseTest {

	@Mock
	private ProductSupport productSupport;

	@Mock
	private AuctionApiClient  auctionApiClient;

	@InjectMocks
	private ProductUpdateProductUseCase useCase;

	private final String PUBLIC_ID = "seller-uuid";
	private final Long PRODUCT_ID = 1L;
	private final Long SELLER_ID = 100L;
	private final Long AUCTION_ID = 100L;

	private ProductMember commonSeller;
	private Product spyProduct;

	@BeforeEach
	void setUp() {
		// 공통 멤버 생성
		commonSeller = ProductMember.builder()
			.id(SELLER_ID)
			.publicId(PUBLIC_ID)
			.build();

		// 공통 상품 생성 및 ID 주입
		Product product = Product.builder()
			.name("기존 이름")
			.build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		// 매 테스트마다 깨끗한 상태의 Spy 객체 준비
		spyProduct = spy(product);
	}

	@Test
	@DisplayName("본인 상품인 경우 정보가 수정되고 상품 ID를 반환한다")
	void updateProduct_success() {
		// given
		ProductUpdateDto updateDto = createUpdateDto("수정된 스타워즈", Category.스타워즈);

		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);
		given(auctionApiClient.updateAuction(eq(PUBLIC_ID), any(ProductAuctionUpdateDto.class)))
			.willReturn(AUCTION_ID);

		// when
		ProductUpdateResponseDto dto = useCase.updateProduct(PUBLIC_ID, PRODUCT_ID, updateDto);

		// then
		assertThat(dto.productId()).isEqualTo(PRODUCT_ID);
		assertThat(dto.auctionId()).isEqualTo(AUCTION_ID);
		verify(spyProduct).update(eq("수정된 스타워즈"), eq(Category.스타워즈), anyString(), anyList());
		verify(productSupport).isAbleToChange(commonSeller, spyProduct);
	}

	@Test
	@DisplayName("본인 상품이 아니면 예외가 발생하고 수정 로직이 실행되지 않는다")
	void updateProduct_fail_notSeller() {
		// given
		ProductUpdateDto updateDto = createUpdateDto("이름", Category.스타워즈);

		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);

		// 권한 체크 실패 시뮬레이션
		willThrow(new CustomException(ErrorType.UNAUTHORIZED_SELLER))
			.given(productSupport).isAbleToChange(any(), any());

		// when & then
		assertThatThrownBy(() -> useCase.updateProduct(PUBLIC_ID, PRODUCT_ID, updateDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED_SELLER);

		verify(spyProduct, never()).update(any(), any(), any(), any());
	}


	private ProductUpdateDto createUpdateDto(String name, Category category) {
		return new ProductUpdateDto(
			name,
			category,
			"설명",
			new ProductAuctionUpdateDto(1L, 1000, 7),
			List.of(new ProductImageUpdateDto(null, "url", 1))
		);
	}

}