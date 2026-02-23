package com.bugzero.rarego.product.in;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.inbox.app.InboxUseCase;
import com.bugzero.rarego.product.app.ProductFacade;
import com.bugzero.rarego.product.app.ProductSearchService;
import com.bugzero.rarego.product.app.ProductSupport;
import com.bugzero.rarego.product.domain.Product;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionRelistedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionStartedEvent;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
	topics = {"auction-management", "member-joined", "member-updated"},
	groupId = "${spring.kafka.consumer.group-id}",
	containerFactory = "kafkaListenerContainerFactory"
)
public class ProductConsumer {
	@Value("${spring.kafka.consumer.group-id}")
	private String consumerGroup;

	private final ProductFacade productFacade;
	private final ProductSearchService productSearchService;
	private final ProductSupport productSupport;
	private final InboxUseCase inboxUseCase;

	/* --- 회원 관련 이벤트 핸들러 (통합됨) --- */

	@Transactional
	@KafkaHandler
	public void onMemberEvent(@Payload MemberJoinedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup))
			return;
		try {
			productFacade.syncMember(event.memberDto());
			log.info("[product] 회원 레플리카 등록 완료 - memberPublicId: {}", event.memberDto().publicId());
		} catch (Exception e) {
			log.error("[product] 회원 레플리카 등록 실패 - memberPublicId: {}", event.memberDto().publicId(), e);
			throw e;
		}
	}

	@Transactional
	@KafkaHandler
	public void onMemberEvent(@Payload MemberUpdatedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup))
			return;
		try {
			productFacade.syncMember(event.memberDto());
			log.info("[product] 회원 레플리카 수정 완료 - memberPublicId: {}", event.memberDto().publicId());
		} catch (Exception e) {
			log.error("[product] 회원 레플리카 수정 실패 - memberPublicId: {}", event.memberDto().publicId(), e);
			throw e;
		}
	}

	/* --- 상품/경매 관련 이벤트 핸들러 --- */

	@Transactional
	@KafkaHandler
	public void onProductEvent(@Payload AuctionStartedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup))
			return;
		try {
			productSearchService.updateAuctionStatus(
				event.productId(),
				event.auctionId(),
				AuctionStatus.IN_PROGRESS
			);

			log.info("[product] 경매 시작 처리 완료 - productId: {}, auctionId: {}",
				event.productId(), event.auctionId());

		} catch (Exception e) {
			log.error("[product] 경매 시작 처리 실패 - productId: {}, auctionId: {}, error: {}",
				event.productId(), event.auctionId(), e.getMessage(), e);
			throw e;
		}
	}

	@Transactional
	@KafkaHandler
	public void onProductEvent(@Payload AuctionEndedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup))
			return;
		try {
			if (event.finalPrice() != null) {
				// 낙찰된 경우
				productSearchService.updateSoldPrice(
					event.productId(),
					event.auctionId(),
					event.finalPrice()
				);
			} else {
				// 유찰된 경우 -> 상태만 ENDED로 변경
				productSearchService.updateAuctionStatus(
					event.productId(),
					event.auctionId(),
					AuctionStatus.ENDED
				);
			}

			log.info("[product] 경매 종료 처리 완료 - productId: {}", event.productId());

		} catch (Exception e) {
			log.error("[product] 경매 종료 처리 실패 - productId: {}, auctionId: {}, error: {}",
				event.productId(), event.auctionId(), e.getMessage(), e);
			throw e;
		}
	}

	@Transactional
	@KafkaHandler
	public void onProductEvent(@Payload AuctionRelistedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup))
			return;
		try {
			// 기존 상품 데이터 조회 (이미지 포함)
			Product product = productSupport.findByIdWithImages(event.productId());

			// ES 문서 갱신 (문서 ID가 같으므로 덮어씌움)
			productSearchService.save(
				product,
				product.getImages(),
				event.newAuctionId(),
				event.startPrice(),
				event.startedAt()
			);

			log.info("[product] 경매 재등록 처리 완료 - productId: {}, newAuctionId: {}",
				event.productId(), event.newAuctionId());

		} catch (Exception e) {
			log.error("[product] 경매 재등록 처리 실패 - productId: {}, newAuctionId: {}, error: {}",
				event.productId(), event.newAuctionId(), e.getMessage(), e);
			throw e;
		}
	}

	@KafkaHandler(isDefault = true)
	public void defaultHandler(Object object) {
		log.warn("[product] 수신된 이벤트 중 처리할 수 없는 타입입니다: {}", object.getClass().getName());
	}
}
