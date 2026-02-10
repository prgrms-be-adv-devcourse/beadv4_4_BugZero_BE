package com.bugzero.rarego.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.NotificationFacade;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentCompletedEvent;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentExpiringSoonEvent;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {
	private final NotificationFacade notificationFacade;
	private static final String GROUP_ID = "${spring.kafka.consumer.group-id}";

	/**
	 * 낙찰 결제 완료
	 */
	@KafkaListener(topics = "payment-auction-completed", groupId = GROUP_ID)
	public void consumePaymentCompleted(AuctionPaymentCompletedEvent event) {
		log.info(">> [Kafka] 결제 완료 이벤트 수신: auctionId={}", event.auctionId());
		notificationFacade.createNotification(event);
	}

	/**
	 * 낙찰 결제 마감 임박
	 */
	@KafkaListener(topics = "payment-auction-expiring-soon", groupId = GROUP_ID)
	public void consumeExpiringSoon(AuctionPaymentExpiringSoonEvent event) {
		log.info(">> [Kafka] 마감 임박 이벤트 수신: auctionId={}", event.auctionId());
		notificationFacade.createNotification(event);
	}

	/**
	 * 정산 완료
	 */
	@KafkaListener(topics = "payment-settlement-finished", groupId = GROUP_ID)
	public void consumeSettlementFinished(SettlementFinishedEvent event) {
		log.info(">> [Kafka] 정산 완료 이벤트 수신: 총 {}건", event.settlements().size());
		notificationFacade.createNotification(event);
	}

	/**
	 * 낙찰 성공
	 */
	@KafkaListener(topics = "auction-ended", groupId = GROUP_ID)
	public void consumeAuctionEnded(AuctionEndedEvent event) {
		log.info(">> [Kafka] 경매 낙찰 이벤트 수신: auctionId={}", event.auctionId());
		notificationFacade.createNotification(event);
	}

	/**
	 * 입찰가 추월
	 */
	// @KafkaListener(topics = "auction-outbid", groupId = GROUP_ID)
	// public void consumeOutbid(OutbidEvent event) {
	// 	log.info(">> [Kafka] 입찰가 추월 이벤트 수신: auctionId={}", event.auctionId());
	// 	notificationService.createNotification(event);
	// }

	/**
	 * 관심 경매 시작
	 */
	// @KafkaListener(topics = "auction-started", groupId = GROUP_ID)
	// public void consumeAuctionStarted(AuctionStartedEvent event) {
	// 	log.info(">> [Kafka] 관심 경매 시작 이벤트 수신: auctionId={}", event.auctionId());
	// 	notificationService.createNotification(event);
	// }
}
