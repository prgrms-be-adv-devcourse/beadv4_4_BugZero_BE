package com.bugzero.rarego.product.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.product.domain.ProductOutbox;
import com.bugzero.rarego.product.out.ProductOutboxRepository;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ProductOutboxSupportTest {
	@Mock
	private ProductOutboxRepository productOutBoxRepository;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private ProductOutboxSupport productOutboxSupport;

	private final Long productId = 100L;
	private final String publicId = "member-uuid-123";

	@Test
	@DisplayName("경매 생성 이벤트 저장 시 필드 값이 포함된 JSON이 생성되어야 한다")
	void saveOutBox_Create_Success() throws JsonProcessingException {
		// given
		ProductAuctionRequestDto dto = ProductAuctionRequestDto.builder()
			.startPrice(5000)
			.durationDays(7)
			.build();

		String expectedPayload = "{\"startPrice\":5000,\"durationDays\":7}";
		given(objectMapper.writeValueAsString(dto)).willReturn(expectedPayload);

		// when
		productOutboxSupport.saveOutbox(productId, publicId, AuctionProductEventType.CREATE, dto);

		// then
		ArgumentCaptor<ProductOutbox> captor = ArgumentCaptor.forClass(ProductOutbox.class);
		verify(productOutBoxRepository).save(captor.capture());

		ProductOutbox savedEvent = captor.getValue();
		assertEquals(expectedPayload, savedEvent.getPayload());
		assertEquals(productId, savedEvent.getProductId());
		assertEquals(AuctionProductEventType.CREATE, savedEvent.getEventType());
	}

	@Test
	@DisplayName("경매 수정 이벤트 저장 시 auctionId가 포함된 페이로드가 저장되어야 한다")
	void saveOutBox_Update_Success() throws JsonProcessingException {
		// given
		ProductAuctionUpdateDto dto = ProductAuctionUpdateDto.builder()
			.auctionId(50L) // 수정할 경매 식별자
			.startPrice(6000)
			.durationDays(3)
			.build();

		String expectedPayload = "{\"auctionId\":50,\"startPrice\":6000,\"durationDays\":3}";
		given(objectMapper.writeValueAsString(dto)).willReturn(expectedPayload);

		// when
		productOutboxSupport.saveOutbox(productId, publicId, AuctionProductEventType.UPDATE, dto);

		// then
		verify(productOutBoxRepository).save(any(ProductOutbox.class));
		verify(objectMapper).writeValueAsString(dto);
	}

	@Test
	@DisplayName("데이터가 없는 경우(삭제 등) 빈 JSON 객체를 저장한다")
	void saveOutBox_NoData_Success() {
		// when
		productOutboxSupport.saveOutbox(productId, publicId, AuctionProductEventType.DELETE);

		// then
		// save 메서드 내부에서 {} 문자열이 들어갔는지 캡처하여 확인
		ArgumentCaptor<ProductOutbox> captor = ArgumentCaptor.forClass(ProductOutbox.class);
		verify(productOutBoxRepository).save(captor.capture());

		assertEquals("{}", captor.getValue().getPayload());
		assertEquals(AuctionProductEventType.DELETE, captor.getValue().getEventType());
	}

	@Test
	@DisplayName("직렬화 실패 시 CustomException(JSON_SERIALIZATION_FAILED)을 던진다")
	void saveOutBox_Serialization_Fail() throws JsonProcessingException {
		// given
		ProductAuctionRequestDto dto = new ProductAuctionRequestDto(5000, 1);
		given(objectMapper.writeValueAsString(dto)).willThrow(JsonProcessingException.class);

		// when & then
		CustomException exception = assertThrows(CustomException.class, () ->
			productOutboxSupport.saveOutbox(productId, publicId, AuctionProductEventType.CREATE, dto)
		);

		assertEquals(ErrorType.JSON_SERIALIZATION_FAILED, exception.getErrorType());
	}
}