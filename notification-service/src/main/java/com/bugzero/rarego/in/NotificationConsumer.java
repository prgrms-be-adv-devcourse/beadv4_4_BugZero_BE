package com.bugzero.rarego.in;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.app.NotificationFacade;
import com.bugzero.rarego.global.inbox.app.InboxUseCase;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionOutbidEvent;
import com.bugzero.rarego.shared.auction.event.AuctionStartedEvent;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentCompletedEvent;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentExpiringSoonEvent;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
	topics = {
		"payment-auction-completed",
		"payment-auction-expiring-soon",
		"payment-settlement-finished",
		"auction-ended",
		"auction-outbid",
		"auction-started",
		"member-joined",
		"member-updated"
	},
	groupId = "${spring.kafka.consumer.group-id}",
	containerFactory = "kafkaListenerContainerFactory"
)
public class NotificationConsumer {
	@Value("${spring.kafka.consumer.group-id}")
	private String consumerGroup;

	private final NotificationFacade notificationFacade;
	private final InboxUseCase inboxUseCase;

	/**
	 * 낙찰 결제 완료
	 */
	@Transactional
	@KafkaHandler
	public void consumePaymentCompleted(
		@Payload AuctionPaymentCompletedEvent event,
		@Header("messageId") String messageId
	) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup)) {
			return;
		}

		log.info(">> [Kafka] 결제 완료 이벤트 수신: auctionId={}", event.auctionId());
		notificationFacade.createNotification(event);
	}

	/**
	 * 낙찰 결제 마감 임박
	 */
	@Transactional
	@KafkaHandler
	public void consumeExpiringSoon(
		@Payload AuctionPaymentExpiringSoonEvent event,
		@Header("messageId") String messageId
	) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup)) {
			return;
		}

		log.info(">> [Kafka] 마감 임박 이벤트 수신: auctionId={}", event.auctionId());
		notificationFacade.createNotification(event);
	}

	/**
	 * 정산 완료
	 */
	@Transactional
	@KafkaHandler
	public void consumeSettlementFinished(
		@Payload SettlementFinishedEvent event,
		@Header("messageId") String messageId
	) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup)) {
			return;
		}

		log.info(">> [Kafka] 정산 완료 이벤트 수신: 총 {}건", event.settlements().size());
		notificationFacade.createNotification(event);
	}

	/**
	 * 낙찰 성공
	 */
	@Transactional
	@KafkaHandler
	public void consumeAuctionEnded(@Payload AuctionEndedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup)) {
			return;
		}

		log.info(">> [Kafka] 경매 낙찰 이벤트 수신: auctionId={}", event.auctionId());
		notificationFacade.createNotification(event);
	}

	/**
	 * 입찰가 추월
	 */
	@Transactional
	@KafkaHandler
	public void consumeOutbid(@Payload AuctionOutbidEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup)) {
			return;
		}

		log.info(">> [Kafka] 입찰가 추월 이벤트 수신: auctionId={}", event.auctionId());
		notificationFacade.createNotification(event);
	}

	/**
	 * 관심 경매 시작
	 */
	@Transactional
	@KafkaHandler
	public void consumeAuctionStarted(@Payload AuctionStartedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup)) {
			return;
		}

		log.info(">> [Kafka] 관심 경매 시작 이벤트 수신: auctionId={}", event.auctionId());
		notificationFacade.createNotification(event);
	}

	/**
	 * notificationMember 생성 동기화
	 */
	@Transactional
	@KafkaHandler
	public void consumeMemberJoined(@Payload MemberJoinedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup)) {
			return;
		}

		try {
			notificationFacade.syncMember(event.memberDto());
			log.info("[notification] 회원 레플리카 등록 완료 - memberPublicId: {}", event.memberDto().publicId());
		} catch (Exception e) {
			log.error("[notification] 회원 레플리카 등록 실패 - memberPublicId: {}", event.memberDto().publicId(), e);
			throw e;
		}
	}

	/**
	 * notificationMember 수정 동기화
	 */
	@Transactional
	@KafkaHandler
	public void consumeMemberUpdated(@Payload MemberUpdatedEvent event, @Header("messageId") String messageId) {
		if (inboxUseCase.isAlreadyProcessed(messageId, consumerGroup)) {
			return;
		}

		try {
			notificationFacade.syncMember(event.memberDto());
			log.info("[notification] 회원 레플리카 수정 완료 - memberPublicId: {}", event.memberDto().publicId());
		} catch (Exception e) {
			log.error("[notification] 회원 레플리카 수정 실패 - memberPublicId: {}", event.memberDto().publicId(), e);
			throw e;
		}
	}

	@KafkaHandler(isDefault = true)
	public void defaultHandler(Object object) {
		log.warn("[Notification] 수신된 이벤트 중 처리할 수 없는 타입입니다: {}", object.getClass().getName());
	}
}
