package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductImageUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateResponseDto;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;
import com.bugzero.rarego.shared.product.event.S3ImageDeleteEvent;

@ExtendWith(MockitoExtension.class)
class ProductUpdateProductUseCaseTest {

	@Mock
	private ProductSupport productSupport;

	@Mock
	private AuctionApiClient auctionApiClient;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private ProductUpdateProductUseCase useCase;

	private final String PUBLIC_ID = "seller-uuid";
	private final Long PRODUCT_ID = 1L;
	private final Long SELLER_ID = 100L;
	private final Long AUCTION_ID = 200L;

	private ProductMember commonSeller;
	private Product spyProduct;

	@BeforeEach
	void setUp() {
		commonSeller = ProductMember.builder()
			.id(SELLER_ID)
			.publicId(PUBLIC_ID)
			.build();

		Product product = Product.builder().name("기존 이름").build();
		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		// Entity의 상태 변화와 메서드 호출을 동시에 추적하기 위해 Spy 사용
		spyProduct = spy(product);
	}

	@Test
	@DisplayName("성공: 상품 정보 수정 시 이미지 삭제 및 확정 이벤트가 각각 발행된다")
	void updateProduct_success() {
		// given
		List<ProductImageUpdateDto> imageDtos = List.of(new ProductImageUpdateDto(null, "temp/new.jpg", 1));
		ProductUpdateDto updateDto = createUpdateDto("수정된 이름", imageDtos);

		List<String> deletePaths = List.of("products/old.jpg");
		List<String> confirmPaths = List.of("temp/new.jpg");

		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);
		given(productSupport.normalizeUpdateImageOrder(anyList())).willReturn(imageDtos);

		// Entity 비즈니스 로직 결과 모킹 (이미지 삭제 및 추가 경로 반환)
		doReturn(deletePaths).when(spyProduct).removeOldImages(anyList());
		doReturn(confirmPaths).when(spyProduct).processNewImages(anyList());

		given(auctionApiClient.updateAuction(eq(PUBLIC_ID), any(ProductAuctionUpdateDto.class)))
			.willReturn(AUCTION_ID);

		// when
		ProductUpdateResponseDto response = useCase.updateProduct(PUBLIC_ID, PRODUCT_ID, updateDto);

		// then
		// 1. 기본 정보 수정 호출 확인
		verify(spyProduct).updateBasicInfo(eq("수정된 이름"), eq(Category.스타워즈), anyString());

		// 2. 이벤트 발행 검증 (가장 중요한 변경점)
		ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher, times(2)).publish(eventCaptor.capture());

		List<Object> publishedEvents = eventCaptor.getAllValues();

		// S3ImageDeleteEvent 검증
		assertThat(publishedEvents.get(0)).isInstanceOf(S3ImageDeleteEvent.class);
		S3ImageDeleteEvent deleteEvent = (S3ImageDeleteEvent) publishedEvents.get(0);
		assertThat(deleteEvent.paths()).isEqualTo(deletePaths);

		// S3ImageConfirmEvent 검증
		assertThat(publishedEvents.get(1)).isInstanceOf(S3ImageConfirmEvent.class);
		S3ImageConfirmEvent confirmEvent = (S3ImageConfirmEvent) publishedEvents.get(1);
		assertThat(confirmEvent.paths()).isEqualTo(confirmPaths);

		// 3. 결과 반환 확인
		assertThat(response.productId()).isEqualTo(PRODUCT_ID);
		assertThat(response.auctionId()).isEqualTo(AUCTION_ID);
	}

	@Test
	@DisplayName("실패: 수정 권한이 없으면 예외가 발생하고 이벤트가 발행되지 않는다")
	void updateProduct_fail_unauthorized() {
		// given
		ProductUpdateDto updateDto = createUpdateDto("이름", Collections.emptyList());

		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);

		willThrow(new CustomException(ErrorType.UNAUTHORIZED_SELLER))
			.given(productSupport).isAbleToChange(any(), any());

		// when & then
		assertThatThrownBy(() -> useCase.updateProduct(PUBLIC_ID, PRODUCT_ID, updateDto))
			.isInstanceOf(CustomException.class);

		// 이벤트가 하나도 발행되지 않았음을 확인
		verifyNoInteractions(eventPublisher);
		verify(spyProduct, never()).updateBasicInfo(any(), any(), any());
	}

	private ProductUpdateDto createUpdateDto(String name, List<ProductImageUpdateDto> images) {
		return new ProductUpdateDto(
			name,
			Category.스타워즈,
			"설명",
			new ProductAuctionUpdateDto(1L, 1000, 7),
			images
		);
	}
}