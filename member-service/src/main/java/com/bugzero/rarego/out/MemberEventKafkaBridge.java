package com.bugzero.rarego.out;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MemberJoinedEvent, MemberUpdatedEvent를 Kafka로 중계하는 브릿지
 * DB 커밋 후에만 전송하여 데이터 일관성 보장
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberEventKafkaBridge {
	private final KafkaTemplate<String, Object> kafkaTemplate;

	private static final String TOPIC_MEMBER_JOINED = "member-joined";
	private static final String TOPIC_MEMBER_UPDATED = "member-updated";

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendToKafka(MemberJoinedEvent event) {
		log.info("🚀 Bridge: Kafka로 이벤트 전송 [Topic: {}]", TOPIC_MEMBER_JOINED);

		// 메시지 순서 보장을 위해 Key를 publicId로 설정
		String key = String.valueOf(event.memberDto().publicId());

		kafkaTemplate.send(TOPIC_MEMBER_JOINED, key, event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendToKafka(MemberUpdatedEvent event) {
		log.info("🚀 Bridge: Kafka로 이벤트 전송 [Topic: {}]", TOPIC_MEMBER_UPDATED);

		// 메시지 순서 보장을 위해 Key를 publicId로 설정
		String key = String.valueOf(event.memberDto().publicId());

		kafkaTemplate.send(TOPIC_MEMBER_UPDATED, key, event);
	}
}
