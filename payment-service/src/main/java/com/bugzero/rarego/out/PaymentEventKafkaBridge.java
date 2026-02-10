package com.bugzero.rarego.out;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.shared.payment.event.AuctionPaymentCompletedEvent;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentExpiringSoonEvent;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventKafkaBridge {
	private final KafkaTemplate<String, Object> kafkaTemplate;

	private static final String TOPIC_SETTLEMENT_FINISHED = "payment-settlement-finished";
	private static final String TOPIC_AUCTION_PAYMENT_COMPLETED = "payment-auction-completed";
	private static final String TOPIC_PAYMENT_EXPIRING_SOON = "payment-auction-expiring-soon";

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendSettlementFinished(SettlementFinishedEvent event) {
		if (event.settlements().isEmpty()) {
			return;
		}

		try {
			log.info("Kafka 발행 시작: 정산 완료 (총 {} 건)", event.totalCount());
			String key = UUID.randomUUID().toString();
			kafkaTemplate.send(TOPIC_SETTLEMENT_FINISHED, key, event);
		} catch (Exception e) {
			log.error("Kafka 발행 실패 (정산): totalCount={}", event.totalCount(), e);
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendAuctionPaymentCompleted(AuctionPaymentCompletedEvent event) {
		try {
			log.info("Kafka 발행 시작: 낙찰 결제 완료 (orderId={})", event.orderId());

			// Key: auctionId (순서 보장)
			String key = String.valueOf(event.auctionId());
			kafkaTemplate.send(TOPIC_AUCTION_PAYMENT_COMPLETED, key, event);

		} catch (Exception e) {
			log.error("Kafka 발행 실패 (결제완료): orderId={}, auctionId={}", event.orderId(), event.auctionId(), e);
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendAuctionPaymentExpiringSoon(AuctionPaymentExpiringSoonEvent event) {
		log.info("결제 마감 임박 이벤트 Kafka 발행 시작: orderId={}, auctionId={}", event.orderId(), event.auctionId());

		try {
			kafkaTemplate.send(TOPIC_PAYMENT_EXPIRING_SOON, String.valueOf(event.auctionId()), event);

		} catch (Exception e) {
			log.error("Kafka 메시지 발행 실패: orderId={}", event.auctionId(), e);
			// 필요 시 여기서 재시도 로직이나 DLQ 처리 (혹은 스케줄러가 다음 턴에 다시 처리하도록 둠)
		}
	}
}
