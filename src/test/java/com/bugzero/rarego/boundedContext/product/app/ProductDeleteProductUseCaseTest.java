package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;

@ExtendWith(MockitoExtension.class)
class ProductDeleteProductUseCaseTest {
	@Mock
	private ProductSupport productSupport;

	@Mock
	private AuctionApiClient auctionApiClient;

	@InjectMocks
	private ProductDeleteProductUseCase useCase;

	private final String PUBLIC_ID = "seller-uuid";
	private final Long PRODUCT_ID = 100L;

	private ProductMember commonSeller;
	private Product spyProduct;

	@BeforeEach
	void setUp() {
		// 공통 판매자 객체 생성
		commonSeller = ProductMember.builder()
			.id(1L)
			.publicId(PUBLIC_ID)
			.build();

		// 공통 상품 객체 생성 및 Spy 설정
		Product product = Product.builder()
			.name("삭제될 상품")
			.build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
		spyProduct = spy(product);
	}

	@Test
	@DisplayName("성공: 모든 검증을 통과하면 상품을 소프트 삭제하고 경매 삭제 API를 호출한다")
	void deleteProduct_Success() {
		// given
		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);
		// isAbleToDelete는 void이므로 문제 없으면 통과

		// when
		useCase.deleteProduct(PUBLIC_ID, PRODUCT_ID);

		// then
		// 1. 엔티티 상태 변경 확인
		verify(spyProduct).softDelete();

		// 2. 외부 API 호출 확인 (가장 중요한 부분)
		verify(auctionApiClient).deleteAuction(PUBLIC_ID, PRODUCT_ID);

		// 3. 권한 체크 수행 여부 확인
		verify(productSupport).isAbleToDelete(commonSeller, spyProduct);
	}

	@Test
	@DisplayName("실패: 삭제 권한이 없으면 예외가 발생하고 외부 API를 호출하지 않는다")
	void deleteProduct_Fail_Unauthorized() {
		// given
		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);

		// 삭제 불가 예외 시뮬레이션
		willThrow(new CustomException(ErrorType.UNAUTHORIZED_SELLER))
			.given(productSupport).isAbleToDelete(any(), any());

		// when & then
		assertThatThrownBy(() -> useCase.deleteProduct(PUBLIC_ID, PRODUCT_ID))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED_SELLER);

		// 핵심: 예외가 발생하면 외부 API 호출이나 엔티티 삭제가 일어나면 안 됨
		verify(spyProduct, never()).softDelete();
		verifyNoInteractions(auctionApiClient);
	}
}