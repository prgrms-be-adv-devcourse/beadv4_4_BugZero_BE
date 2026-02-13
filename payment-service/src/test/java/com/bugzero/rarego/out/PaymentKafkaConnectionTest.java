package com.bugzero.rarego.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import com.bugzero.rarego.shared.payment.dto.SettlementResponseDto;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentCompletedEvent;
import com.bugzero.rarego.shared.payment.event.AuctionPaymentExpiringSoonEvent;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;

@SpringBootTest(classes = {KafkaAutoConfiguration.class})
@DirtiesContext
@EmbeddedKafka(
	partitions = 1,
	topics = {
		"payment-settlement-finished",
		"payment-auction-completed",
		"payment-auction-expiring-soon"
	}
)
@TestPropertySource(properties = {
	"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
	"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
	"spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer"
})
class PaymentKafkaConnectionTest {

	private static final String TOPIC_SETTLEMENT = "payment-settlement-finished";
	private static final String TOPIC_PAYMENT_COMPLETED = "payment-auction-completed";
	private static final String TOPIC_EXPIRING_SOON = "payment-auction-expiring-soon";
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Test
	@DisplayName("1. 정산 완료 이벤트(SettlementFinishedEvent) 전송 테스트")
	void testSend_SettlementFinished() {
		SettlementResponseDto dto1 = new SettlementResponseDto(
			1L, 100L, 200L, 10000, 1000, 9000, "COMPLETED", LocalDateTime.now()
		);
		List<SettlementResponseDto> settlements = List.of(dto1);
		SettlementFinishedEvent event = new SettlementFinishedEvent(settlements, 1, 9000);

		sendAndLog(TOPIC_SETTLEMENT, UUID.randomUUID().toString(), event);
	}

	@Test
	@DisplayName("2. 낙찰 결제 완료 이벤트(AuctionPaymentCompletedEvent) 전송 테스트")
	void testSend_AuctionPaymentCompleted() {
		AuctionPaymentCompletedEvent event = new AuctionPaymentCompletedEvent(
			123L, 456L, 999L, 789L, 15000
		);
		sendAndLog(TOPIC_PAYMENT_COMPLETED, String.valueOf(event.auctionId()), event);
	}

	@Test
	@DisplayName("3. 마감 임박 알림 이벤트(AuctionPaymentExpiringSoonEvent) 전송 테스트")
	void testSend_ExpiringSoon() {
		AuctionPaymentExpiringSoonEvent event = new AuctionPaymentExpiringSoonEvent(
			111L, 222L, 333L, 444L, 20000, LocalDateTime.now().plusHours(12)
		);
		sendAndLog(TOPIC_EXPIRING_SOON, String.valueOf(event.auctionId()), event);
	}

	private void sendAndLog(String topic, String key, Object event) {
		try {
			System.out.println(">>> 전송 시도: Topic=" + topic + ", Key=" + key);

			// Embedded Kafka는 로컬 통신이므로 타임아웃을 5~10초 정도로 넉넉히 잡는 것이 안전합니다.
			SendResult<String, Object> result = kafkaTemplate.send(topic, key, event)
				.get(10, TimeUnit.SECONDS);

			System.out.println("✅ 전송 성공!");
			System.out.println("   - Offset: " + result.getRecordMetadata().offset());
			System.out.println("   - Partition: " + result.getRecordMetadata().partition());
			System.out.println("--------------------------------------------------");

		} catch (Exception e) {
			System.err.println("❌ 전송 실패: " + e.getMessage());
			throw new RuntimeException("Kafka 전송 실패", e);
		}
	}
}
