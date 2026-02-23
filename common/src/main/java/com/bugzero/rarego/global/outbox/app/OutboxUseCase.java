package com.bugzero.rarego.global.outbox.app;

import static com.bugzero.rarego.global.response.ErrorType.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.kafka.KafkaTopics;
import com.bugzero.rarego.global.outbox.domain.OutboxEvent;
import com.bugzero.rarego.global.outbox.domain.OutboxStatus;
import com.bugzero.rarego.global.outbox.repository.OutboxEventRepository;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionOutbidEvent;
import com.bugzero.rarego.shared.auction.event.AuctionRelistedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionStartedEvent;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentCompletedEvent;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentExpiringSoonEvent;
import com.bugzero.rarego.shared.payment.event.PaymentTimeoutEvent;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;
import com.bugzero.rarego.shared.product.event.ProductCreateAuctionEvent;
import com.bugzero.rarego.shared.product.event.ProductDeleteAuctionEvent;
import com.bugzero.rarego.shared.product.event.ProductUpdateAuctionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxUseCase {

	private final OutboxEventRepository outboxEventRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@Value("${outbox.poller.batch-size:100}") // 각 모듈에 적합하게 설정
	private int batchSize;

	@Value("${outbox.poller.max-retry:5}") // 각 모듈에 적합하게 설정
	private int maxRetry;

	@Transactional(propagation = Propagation.MANDATORY)
	public void saveOutbox(Object event) {
		OutboxEventMetadata metadata = resolveMetadata(event);

		if (metadata == null) {
			log.debug("해당 이벤트를 위한 아웃박스 메타데이터가 없습니다: {}", event.getClass().getSimpleName());
			throw new CustomException(UNSUPPORTED_OUTBOX_EVENT);
		}

		try {
			String payload = objectMapper.writeValueAsString(event);

			OutboxEvent outboxEvent = OutboxEvent.createOutboxEvent(
				metadata.aggregateType(),
				metadata.aggregateId(),
				event.getClass().getSimpleName(),
				metadata.topic(),
				payload
			);

			outboxEventRepository.save(outboxEvent);
			log.debug("Outbox에 다음 이벤트를 저장했습니다. 이벤트 : {}",
				metadata.aggregateType());

		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorType.JSON_SERIALIZATION_FAILED);
		}
	}

	// 카프카로 보낼 이벤트 메타데이터 생성
	private OutboxEventMetadata resolveMetadata(Object event) {
		return switch (event) {
			case MemberJoinedEvent e -> new OutboxEventMetadata(
				"Member", String.valueOf(e.memberDto().id()), KafkaTopics.MEMBER_JOINED.getTopicName());
			case MemberUpdatedEvent e -> new OutboxEventMetadata(
				"Member", String.valueOf(e.memberDto().id()), KafkaTopics.MEMBER_UPDATE.getTopicName());
			case AuctionEndedEvent e -> new OutboxEventMetadata(
				"Auction", generateId(e.productId(), e.auctionId()), KafkaTopics.AUCTION_ENDED.getTopicName());
			case AuctionStartedEvent e -> new OutboxEventMetadata(
				"Auction", generateId(e.productId(), e.auctionId()), KafkaTopics.AUCTION_STARTED.getTopicName());
			case AuctionRelistedEvent e -> new OutboxEventMetadata(
				"Auction", generateId(e.productId(), e.newAuctionId()), KafkaTopics.AUCTION_RELISTED.getTopicName());
			case AuctionOutbidEvent e -> new OutboxEventMetadata(
				"Auction", String.valueOf(e.auctionId()), KafkaTopics.AUCTION_OUTBID.getTopicName());
			case SettlementFinishedEvent e -> new OutboxEventMetadata(
				"Payment", "settlement_payment", KafkaTopics.PAYMENT_SETTLEMENT_FINISHED.getTopicName());
			case AuctionPaymentCompletedEvent e -> new OutboxEventMetadata(
				"Payment", String.valueOf(e.sellerId()), KafkaTopics.PAYMENT_AUCTION_COMPLETED.getTopicName());
			case AuctionPaymentExpiringSoonEvent e -> new OutboxEventMetadata(
				"Payment", String.valueOf(e.buyerId()), KafkaTopics.PAYMENT_AUCTION_EXPIRING_SOON.getTopicName());
			case PaymentTimeoutEvent e -> new OutboxEventMetadata(
				"Payment", String.valueOf(e.auctionId()), KafkaTopics.PAYMENT_TIMEOUT.getTopicName());
			case ProductCreateAuctionEvent e -> new OutboxEventMetadata(
				"Product", String.valueOf(e.productId()), KafkaTopics.AUCTION_INFO_MANAGEMENT.getTopicName());
			case ProductUpdateAuctionEvent e -> new OutboxEventMetadata(
				"Product", String.valueOf(e.productId()), KafkaTopics.AUCTION_INFO_MANAGEMENT.getTopicName());
			case ProductDeleteAuctionEvent e -> new OutboxEventMetadata(
				"Product", String.valueOf(e.productId()), KafkaTopics.AUCTION_INFO_MANAGEMENT.getTopicName());
			default -> null;
		};
	}

	private record OutboxEventMetadata(String aggregateType, String aggregateId, String topic) {
	}

	public String generateId(Long productId, Long auctionId) {
		return productId + "_" + auctionId;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int deleteSentEventsBatch(LocalDateTime threshold, int batchSize) {
		return outboxEventRepository.deleteSentEventsBatch(threshold, batchSize);
	}

	// ID 리스트 조회 (트랜잭션 없이 혹은 기본 Read Only)
	public List<Long> findPendingEventIds() {
		return outboxEventRepository.findIdsByStatusOrderByCreatedAt(
			OutboxStatus.PENDING, PageRequest.of(0, batchSize));
	}

	 // 개별 메시지를 전송하고 상태 업데이트
	 // REQUIRES_NEW를 통해 각 메시지 처리가 성공할 때마다 즉시 DB에 커밋
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processAndPublish(Long eventId) {
		OutboxEvent event = outboxEventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(ErrorType.OUTBOX_NOT_FOUND));

		try {
			event.markProcessing();

			// 카프카 전송용 레코드 생성 (헤더 포함)
			ProducerRecord<String, Object> record = createRecord(event);

			// 동기 전송 (결과가 올 때까지 기다림)
			kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);

			// 전송 성공 시 상태 변경 (메서드 종료 시 커밋됨)
			event.markSent();

		} catch (Exception e) {
			log.error("[Outbox] 전송 실패: id={}, error={}", event.getId(), e.getMessage());
			// 실패 상태 반영 (메서드 종료 시 커밋됨)
			event.markFailed(e.getMessage(), maxRetry);
		}
	}

	private ProducerRecord<String, Object> createRecord(OutboxEvent event) {
		ProducerRecord<String, Object> record = new ProducerRecord<>(
			event.getTopic(),
			event.getAggregateId(),
			event.getPayload()
		);
		record.headers().add("__TypeId__", event.getEventType().getBytes(StandardCharsets.UTF_8));
		record.headers().add("messageId", event.generateMessageId().getBytes(StandardCharsets.UTF_8));
		record.headers().add("aggregateType", event.getAggregateType().getBytes(StandardCharsets.UTF_8));
		return record;
	}
}
