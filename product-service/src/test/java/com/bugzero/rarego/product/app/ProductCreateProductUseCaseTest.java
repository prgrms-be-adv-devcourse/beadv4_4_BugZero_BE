package com.bugzero.rarego.product.app;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.domain.dto.ProductCreateResponseDto;
import com.bugzero.rarego.product.out.ProductRepository;
import com.bugzero.rarego.shared.auction.event.AuctionManagementEvent;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;
import com.bugzero.rarego.shared.product.type.Category;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private ObjectMapper objectMapper;

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Test
	@DisplayName("성공: 상품이 등록되면 DB 저장, S3 이벤트 발행, 그리고 카프카로 경매 생성 메시지를 전송한다")
	void createProduct_success() throws JsonProcessingException {
		// given
		String memberUUID = "seller-uuid";
		String tempUrl = "temp/starwars.jpg";
		String mockPayload = "{\"price\":1000,\"duration\":7}";

		ProductCreateRequestDto request = new ProductCreateRequestDto(
			"스타워즈 시리즈",
			Category.STARWARS,
			"설명",
			new ProductAuctionRequestDto(1000, 7),
			List.of(new ProductImageRequestDto(tempUrl, 0))
		);

		ProductMember seller = ProductMember.builder().id(1L).build();

		given(productSupport.verifyValidateMember(memberUUID)).willReturn(seller);
		given(productSupport.normalizeCreateImageOrder(anyList())).willReturn(request.productImageRequestDto());

		given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
			Product product = invocation.getArgument(0);
			ReflectionTestUtils.setField(product, "id", 1L);
			return product;
		});

		// ObjectMapper가 DTO를 String으로 변환하는 동작 모킹
		given(objectMapper.writeValueAsString(any(ProductAuctionRequestDto.class))).willReturn(mockPayload);

		// when
		ProductCreateResponseDto response = useCase.createProduct(memberUUID, request);

		// then
		// 1. DB 저장 검증
		verify(productRepository).save(any(Product.class));

		// 2. S3 이미지 확정 이벤트 발행 검증
		verify(eventPublisher).publish(any(S3ImageConfirmEvent.class));

		// 3. 카프카 메시지 발행 검증 (핵심 변경 사항)
		ArgumentCaptor<AuctionManagementEvent> kafkaEventCaptor = ArgumentCaptor.forClass(AuctionManagementEvent.class);

		// kafkaTemplate.send(topic, key, value) 호출 검증
		verify(kafkaTemplate).send(
			eq("auction-product-events"),
			eq("1"), // savedProduct.getId().toString()
			kafkaEventCaptor.capture()
		);

		AuctionManagementEvent capturedEvent = kafkaEventCaptor.getValue();
		assertThat(capturedEvent.eventType()).isEqualTo(AuctionProductEventType.CREATE);
		assertThat(capturedEvent.productId()).isEqualTo(1L);
		assertThat(capturedEvent.publicId()).isEqualTo(memberUUID);
		assertThat(capturedEvent.payload()).isEqualTo(mockPayload);
		assertThat(capturedEvent.requestId()).startsWith("REQ-");

		// 4. 응답값 검증
		assertThat(response.productId()).isEqualTo(1L);
	}
}