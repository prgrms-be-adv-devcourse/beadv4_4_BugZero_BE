package com.bugzero.rarego.out;

import java.time.LocalDateTime;
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

import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionRelistedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionStartedEvent;

@SpringBootTest(classes = {KafkaAutoConfiguration.class})
@DirtiesContext
@EmbeddedKafka(
	partitions = 1,
	topics = {
		"auction-ended",
		"auction-relisted",
		"auction-started"
	}
)
@TestPropertySource(properties = {
	"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
	"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
	"spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer"
})
class AuctionKafkaConnectionTest {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	// 토픽명 상수화
	private static final String TOPIC_AUCTION_ENDED = "auction-ended";
	private static final String TOPIC_AUCTION_RELISTED = "auction-relisted";
	private static final String TOPIC_AUCTION_STARTED = "auction-started";

	@Test
	@DisplayName("로컬 카프카로 실제 경매 종료 메시지 전송 테스트")
	void testAuctionEndedRealSend() {
		AuctionEndedEvent event = new AuctionEndedEvent(999L, 777L, 150000, 888L);
		sendAndLog(TOPIC_AUCTION_ENDED, "999", event);
	}

	@Test
	@DisplayName("로컬 카프카로 실제 재등록 메시지 전송 테스트")
	void testRelistRealSend() {
		AuctionRelistedEvent event = new AuctionRelistedEvent(
			999L, 1001L, 120000, LocalDateTime.now()
		);
		sendAndLog(TOPIC_AUCTION_RELISTED, "999", event);
	}

	@Test
	@DisplayName("로컬 카프카로 실제 경매 시작 메시지 전송 테스트")
	void testAuctionStartedRealSend() {
		AuctionStartedEvent event = new AuctionStartedEvent(
			777L, 999L, LocalDateTime.now()
		);
		sendAndLog(TOPIC_AUCTION_STARTED, "777", event);
	}

	/**
	 * 전송 및 결과 로그 출력 헬퍼 메서드
	 */
	private void sendAndLog(String topic, String key, Object event) {
		try {
			System.out.println(">>> 전송 시도: Topic=" + topic + ", Key=" + key);

			SendResult<String, Object> result = kafkaTemplate.send(topic, key, event)
				.get(3, TimeUnit.SECONDS); // 타임아웃 추가

			System.out.println("✅ 전송 성공!");
			System.out.println("   - Offset: " + result.getRecordMetadata().offset());
			System.out.println("   - Partition: " + result.getRecordMetadata().partition());
			System.out.println("--------------------------------------------------");

		} catch (Exception e) {
			System.err.println("❌ 전송 실패: " + e.getMessage());
			throw new RuntimeException("Kafka 전송 실패", e); // 테스트 실패 처리
		}
	}
}
