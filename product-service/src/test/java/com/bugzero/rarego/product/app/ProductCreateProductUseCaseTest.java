package com.bugzero.rarego.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
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

import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.domain.dto.ProductCreateResponseDto;
import com.bugzero.rarego.product.out.ProductRepository;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;
import com.bugzero.rarego.shared.product.type.Category;

@ExtendWith(MockitoExtension.class)
class ProductCreateProductUseCaseTest {

	@InjectMocks
	private ProductCreateProductUseCase useCase;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductSupport productSupport;

	@Mock
	private EventPublisher eventPublisher;

	@Mock
	private ProductOutboxSupport productOutboxSupport;

	private ProductCreateRequestDto request;

	private String memberUUID;

	@BeforeEach
	void setUp() {
		// given
		memberUUID = "seller-uuid";
		String tempUrl = "temp/starwars.jpg";

		request = new ProductCreateRequestDto(
			"스타워즈 시리즈",
			Category.STARWARS,
			"설명",
			new ProductAuctionRequestDto(1000, 7),
			List.of(new ProductImageRequestDto(tempUrl, 0))
		);
	}

	@Test
	@DisplayName("성공: 상품이 등록되면 DB 저장, 아웃박스 기록, 그리고 S3 이벤트를 발행한다")
	void createProduct_success() {

		ProductMember seller = ProductMember.builder().id(1L).build();

		given(productSupport.verifyValidateMember(memberUUID)).willReturn(seller);
		given(productSupport.normalizeCreateImageOrder(anyList())).willReturn(request.productImageRequestDto());

		given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
			Product product = invocation.getArgument(0);
			ReflectionTestUtils.setField(product, "id", 100L); // 상품 ID를 100L로 설정
			return product;
		});

		// when
		ProductCreateResponseDto response = useCase.createProduct(memberUUID, request);

		// then
		// 1. 상품 엔티티 DB 저장 검증
		verify(productRepository).save(any(Product.class));

		// 2. 아웃박스 저장 검증 (핵심 변경 사항)
		// 이제 유스케이스는 직접 카프카를 쏘지 않고 Support 클래스를 호출함
		verify(productOutboxSupport).saveOutbox(
			eq(100L),                         // savedProduct.getId()
			eq(memberUUID),                   // publicId
			eq(AuctionProductEventType.CREATE), // type
			eq(request.productAuctionRequestDto()) // dto
		);

		// 3. S3 이미지 확정 이벤트 발행 검증
		verify(eventPublisher).publish(any(S3ImageConfirmEvent.class));

		// 4. 응답값 검증
		assertThat(response.productId()).isEqualTo(100L);
	}

	@Test
	@DisplayName("실패: 존재하지 않는 회원인 경우 상품 등록이 실패한다")
	void createProduct_fail_memberNotFound() {
		// given
		String invalidPublicId = "wrong-id";

		// productSupport에서 예외를 던지도록 설정
		given(productSupport.verifyValidateMember(invalidPublicId))
			.willThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// when & then
		CustomException exception = assertThrows(CustomException.class, () ->
			useCase.createProduct(invalidPublicId, request)
		);

		assertThat(exception.getErrorType()).isEqualTo(ErrorType.MEMBER_NOT_FOUND);

		// 검증: 이후 로직(저장, 아웃박스 등)이 실행되지 않아야 함
		verify(productRepository, never()).save(any());
		verify(productOutboxSupport, never()).saveOutbox((Long)any(), (String)any(), (AuctionProductEventType)any(),
			(ProductAuctionRequestDto)any());
	}

	@Test
	@DisplayName("실패: 아웃박스 저장 중 예외가 발생하면 전체 프로세스가 실패한다")
	void createProduct_fail_outboxError() {
		// given
		memberUUID = "seller-uuid";
		ProductMember seller = ProductMember.builder().id(1L).build();

		given(productSupport.verifyValidateMember(memberUUID)).willReturn(seller);
		given(productRepository.save(any(Product.class))).willAnswer(inv -> {
			Product p = inv.getArgument(0);
			ReflectionTestUtils.setField(p, "id", 100L);
			return p;
		});

		// 아웃박스 저장 시 예외 발생 모킹
		doThrow(new CustomException(ErrorType.JSON_SERIALIZATION_FAILED))
			.when(productOutboxSupport).saveOutbox(anyLong(), anyString(), (AuctionProductEventType)any(),
				(ProductAuctionRequestDto)any());

		// when & then
		assertThrows(CustomException.class, () ->
			useCase.createProduct(memberUUID, request)
		);

		// 중요: 아웃박스가 실패하면 S3 이벤트 등 후속 작업이 실행되지 않아야 함
		verify(eventPublisher, never()).publish(any(S3ImageConfirmEvent.class));
	}
}