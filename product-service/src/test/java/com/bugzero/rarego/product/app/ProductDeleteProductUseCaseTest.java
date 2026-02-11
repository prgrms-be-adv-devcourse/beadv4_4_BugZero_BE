package com.bugzero.rarego.product.app;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;

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
import com.bugzero.rarego.product.domain.ProductImage;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.event.S3ImageDeleteEvent;

@ExtendWith(MockitoExtension.class)
class ProductDeleteProductUseCaseTest {

	@Mock
	private ProductSupport productSupport;

	@Mock
	private ProductOutboxSupport productOutboxSupport; // 핵심 변경: API 클라이언트 대신 아웃박스 서포트 주입

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private ProductDeleteProductUseCase useCase;

	private final String PUBLIC_ID = "seller-uuid";
	private final Long PRODUCT_ID = 100L;

	private ProductMember commonSeller;
	private Product spyProduct;

	@BeforeEach
	void setUp() {
		commonSeller = ProductMember.builder()
			.id(1L)
			.publicId(PUBLIC_ID)
			.build();

		// 테스트용 이미지들을 포함한 상품 생성
		Product product = Product.builder()
			.name("삭제될 상품")
			.images(new ArrayList<>()) // 가변 리스트로 초기화
			.build();

		product.addImage(ProductImage.createConfirmedImage(product, "products/image1.jpg", 0));
		product.addImage(ProductImage.createConfirmedImage(product, "products/image2.jpg", 1));

		ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

		spyProduct = spy(product);
	}

	@Test
	@DisplayName("성공: 상품 삭제 시 소프트 삭제를 수행하고 아웃박스 저장 및 S3 삭제 이벤트를 발행한다")
	void deleteProduct_Success() {
		// given
		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.findByIdWithImages(PRODUCT_ID)).willReturn(spyProduct);

		// when
		useCase.deleteProduct(PUBLIC_ID, PRODUCT_ID);

		// then
		// 1. 엔티티 행위 검증
		verify(spyProduct).softDelete();
		assertThat(spyProduct.getImages()).isEmpty(); // clear() 호출 확인

		// 2. 아웃박스 저장 검증 (가장 중요한 변경점)
		// 삭제는 DTO가 없는 오버로딩 메서드가 호출되어야 함
		verify(productOutboxSupport).saveOutbox(
			eq(PRODUCT_ID),
			eq(PUBLIC_ID),
			eq(AuctionProductEventType.DELETE)
		);

		// 3. S3 이미지 삭제 이벤트 발행 검증 (커밋 후 실행될 이벤트)
		ArgumentCaptor<S3ImageDeleteEvent> eventCaptor = ArgumentCaptor.forClass(S3ImageDeleteEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());

		S3ImageDeleteEvent publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent.paths()).containsExactly("products/image1.jpg", "products/image2.jpg");

		// 4. 권한 체크 수행 여부 확인
		verify(productSupport).isAbleToDelete(commonSeller, spyProduct);
	}

	@Test
	@DisplayName("실패: 삭제 권한이 없으면 아웃박스 저장 및 S3 이벤트를 발행하지 않는다")
	void deleteProduct_Fail_Unauthorized() {
		// given
		given(productSupport.verifyValidateMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productSupport.findByIdWithImages(PRODUCT_ID)).willReturn(spyProduct);

		willThrow(new CustomException(ErrorType.UNAUTHORIZED_SELLER))
			.given(productSupport).isAbleToDelete(any(), any());

		// when & then
		assertThatThrownBy(() -> useCase.deleteProduct(PUBLIC_ID, PRODUCT_ID))
			.isInstanceOf(CustomException.class);

		// 검증: 예외 발생 시 DB 상태를 변경하거나 외부에 알리는 로직이 실행되지 않아야 함
		verify(spyProduct, never()).softDelete();
		verifyNoInteractions(productOutboxSupport); // 아웃박스 저장 안됨
		verifyNoInteractions(eventPublisher);       // S3 이벤트 발행 안됨
	}
}