package com.bugzero.rarego.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.AuctionFacade;
import com.bugzero.rarego.shared.auction.event.AuctionManagementEvent;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionProductEventListener {

	private final AuctionFacade auctionFacade;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "auction-product-events", groupId = "auction-service-group")
	public void onAuctionEvent(AuctionManagementEvent event) {
		log.info("카프카 이벤트 수신: Type={}, ProductId={}, RequestId={}",
			event.eventType(), event.productId(), event.requestId());
		//TODO 아웃박스 패턴 도입 시 requestId 추가 멱등성 체크를 하도록 설계
		try {
			switch (event.eventType()) {
				case AuctionProductEventType.CREATE -> {
					ProductAuctionRequestDto createDto = objectMapper.readValue(
						event.payload(), ProductAuctionRequestDto.class);
					auctionFacade.createAuction(event.productId(), event.publicId(), createDto);
				}
				case AuctionProductEventType.UPDATE -> {
					ProductAuctionUpdateDto updateDto = objectMapper.readValue(
						event.payload(), ProductAuctionUpdateDto.class);
					auctionFacade.updateAuction(event.publicId(), updateDto);
				}
				case AuctionProductEventType.DELETE -> auctionFacade.deleteAuction(event.publicId(), event.productId());

				default -> log.warn("알 수 없는 이벤트 타입 수신: {}", event.eventType());
			}
		} catch (JsonProcessingException e) {
			log.error("이 메시지는 데이터가 잘못되어 복구 불가능합니다. 로그만 남기고 넘깁니다.", e);
		} catch (Exception e) {
			log.error("이벤트 처리 중 예상치 못한 에러 발생: {}", e.getMessage());
			throw e;
		}
	}
}
