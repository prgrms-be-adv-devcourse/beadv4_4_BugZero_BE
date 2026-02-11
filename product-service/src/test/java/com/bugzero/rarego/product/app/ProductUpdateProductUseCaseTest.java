package com.bugzero.rarego.product.app;

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

import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.domain.dto.ProductUpdateResponseDto;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductImageUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;
import com.bugzero.rarego.shared.product.event.S3ImageDeleteEvent;
import com.bugzero.rarego.shared.product.type.Category;

@ExtendWith(MockitoExtension.class)
class ProductUpdateProductUseCaseTest {

	@Mock
	private ProductSupport productSupport;

	@Mock
	private ProductOutboxSupport productOutboxSupport; // 핵심 변경: API 클라이언트 대신 아웃박스 서포트 주입

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private ProductUpdateProductUseCase useCase;

	private final String PUBLIC_ID = "seller-uuid";
	private final Long PRODUCT_ID = 1L;
	private final Long SELLER_ID = 100L;

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
	@DisplayName("성공: 상품 정보 수정 시 아웃박스에 저장되고 이미지 삭제/확정 이벤트가 각각 발행된다")
	void updateProduct_success() {
		// given
		List<ProductImageUpdateDto> imageDtos = List.of(new ProductImageUpdateDto(null, "temp/new.jpg", 1));
		ProductUpdateDto updateDto = createUpdateDto("수정된 이름", imageDtos);

		List<String> deletePaths = List.of("products/old.jpg");
		List<String> confirmPaths = List.of("temp/new.jpg");

		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.verifyValidateProduct(PRODUCT_ID)).willReturn(spyProduct);
		given(productSupport.normalizeUpdateImageOrder(anyList())).willReturn(imageDtos);

		// Entity 비즈니스 로직 결과 모킹
		doReturn(deletePaths).when(spyProduct).removeOldImages(anyList());
		doReturn(confirmPaths).when(spyProduct).processNewImages(anyList());

		// when
		ProductUpdateResponseDto response = useCase.updateProduct(PUBLIC_ID, PRODUCT_ID, updateDto);

		// then
		// 1. 기본 정보 수정 호출 확인
		verify(spyProduct).updateBasicInfo(eq("수정된 이름"), eq(Category.STARWARS), anyString());

		// 2. 아웃박스 저장 검증 (가장 중요한 변경점)
		verify(productOutboxSupport).saveOutbox(
			eq(PRODUCT_ID),
			eq(PUBLIC_ID),
			eq(AuctionProductEventType.UPDATE),
			eq(updateDto.productAuctionUpdateDto())
		);

		// 3. S3 이벤트 발행 검증 (순서상 아웃박스 저장 후 호출됨)
		ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher, times(2)).publish(eventCaptor.capture());

		List<Object> publishedEvents = eventCaptor.getAllValues();
		assertThat(publishedEvents.get(0)).isInstanceOf(S3ImageDeleteEvent.class);
		assertThat(publishedEvents.get(1)).isInstanceOf(S3ImageConfirmEvent.class);

		// 4. 결과 반환 확인
		assertThat(response.productId()).isEqualTo(PRODUCT_ID);
	}

	@Test
	@DisplayName("실패: 수정 권한이 없으면 아웃박스 저장 및 이벤트 발행이 수행되지 않는다")
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

		// 검증: 핵심 행위들이 전혀 실행되지 않았어야 함
		verifyNoInteractions(productOutboxSupport);
		verifyNoInteractions(eventPublisher);
		verify(spyProduct, never()).updateBasicInfo(any(), any(), any());
	}

	private ProductUpdateDto createUpdateDto(String name, List<ProductImageUpdateDto> images) {
		return new ProductUpdateDto(
			name,
			Category.STARWARS,
			"설명",
			new ProductAuctionUpdateDto(1L, 1000, 7), // auctionId=1L
			images
		);
	}
}