package com.bugzero.rarego.product.app;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.product.domain.ProductOutbox;
import com.bugzero.rarego.product.out.ProductOutboxRepository;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductOutboxSupport {

	private final ProductOutboxRepository productOutBoxRepository;
	private final ObjectMapper objectMapper;

	// 1. 생성 시 사용
	public void saveOutbox(Long productId, String publicId, AuctionProductEventType type, ProductAuctionRequestDto dto) {
		save(productId, publicId, type, serialize(dto));
	}

	// 2. 수정 시 사용
	public void saveOutbox(Long productId, String publicId, AuctionProductEventType type, ProductAuctionUpdateDto dto) {
		save(productId, publicId, type, serialize(dto));
	}

	// 2. 삭제 시 사용
	public void saveOutbox(Long productId, String publicId, AuctionProductEventType type) {
		save(productId, publicId, type, "{}");
	}

	// 공통 저장 로직 (private)
	private void save(Long productId, String publicId, AuctionProductEventType type, String payload) {
		ProductOutbox outbox = ProductOutbox.createNewEvent(productId, publicId, type, payload);
		productOutBoxRepository.save(outbox);
	}

	private String serialize(Object dto) {
		try {
			return objectMapper.writeValueAsString(dto);
		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorType.JSON_SERIALIZATION_FAILED);
		}
	}
}
