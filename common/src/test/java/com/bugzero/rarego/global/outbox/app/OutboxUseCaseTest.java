package com.bugzero.rarego.global.outbox.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.kafka.KafkaTopics;
import com.bugzero.rarego.global.outbox.domain.OutboxEvent;
import com.bugzero.rarego.global.outbox.domain.OutboxStatus;
import com.bugzero.rarego.global.outbox.repository.OutboxEventRepository;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionCreateDto;
import com.bugzero.rarego.shared.product.event.ProductCreateAuctionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxUseCaseTest {

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	@InjectMocks
	private OutboxUseCase outboxUseCase;

	@Test
	@DisplayName("상품 경매 생성 이벤트가 들어오면 Product 메타데이터를 추출하고 저장한다")
	void should_SaveOutboxEvent_When_ProductCreateAuctionEventProvided() throws JsonProcessingException {
		// given
		// 1. 테스트용 DTO 및 이벤트 생성 (Builder 활용)
		ProductAuctionCreateDto dto = ProductAuctionCreateDto.builder()
			.startPrice(1000)
			.durationDays(7)
			.build();

		ProductCreateAuctionEvent event = ProductCreateAuctionEvent.builder()
			.productId(101L)
			.publicId("PROD-ABC-123")
			.dto(dto)
			.build();

		String expectedPayload = "{\"productId\":101, \"startPrice\":1000, \"durationDays\":7}";

		// ObjectMapper 동작 정의
		when(objectMapper.writeValueAsString(event)).thenReturn(expectedPayload);

		// when
		outboxUseCase.saveOutbox(event);

		// then
		// resolveMetadata의 'Product' 분기 로직 검증
		verify(outboxEventRepository).save(argThat(outboxEvent ->
			outboxEvent.getAggregateType().equals("Product") && // 메타데이터 타입 확인
				outboxEvent.getAggregateId().equals("101") &&       // productId가 식별자로 쓰였는지 확인
				outboxEvent.getTopic().equals(KafkaTopics.AUCTION_INFO_MANAGEMENT.getTopicName()) && // 토픽 확인
				outboxEvent.getPayload().equals(expectedPayload)    // 직렬화된 데이터 확인
		));
	}

	@Test
	@DisplayName("지원하지 않는 일반 객체가 들어오면 UNSUPPORTED_OUTBOX_EVENT 예외를 던진다")
	void should_ThrowException_When_UnsupportedEventProvided() {
		// given
		Object unsupportedEvent = new Object();

		// when & then
		CustomException exception = assertThrows(CustomException.class, () ->
			outboxUseCase.saveOutbox(unsupportedEvent)
		);
		assertThat(exception.getErrorType()).isEqualTo(ErrorType.UNSUPPORTED_OUTBOX_EVENT);
	}

	@Test
	@DisplayName("JSON 직렬화 실패 시 JSON_SERIALIZATION_FAILED 예외를 던진다")
	void should_ThrowException_When_JsonSerializationFails() throws JsonProcessingException {
		// given
		ProductCreateAuctionEvent event = ProductCreateAuctionEvent.builder()
			.productId(101L)
			.build();

		when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

		// when & then
		CustomException exception = assertThrows(CustomException.class, () ->
			outboxUseCase.saveOutbox(event)
		);
		assertThat(exception.getErrorType()).isEqualTo(ErrorType.JSON_SERIALIZATION_FAILED);
	}

	@Test
	@DisplayName("이벤트를 정상적으로 카프카로 전송하고 상태를 SENT로 변경한다")
	void should_MarkAsSent_When_PublishSucceeds() throws Exception {
		// given
		Long eventId = 1L;
		// 테스트용 엔티티 생성 (createOutboxEvent 정적 팩토리 메서드 활용)
		OutboxEvent event = OutboxEvent.createOutboxEvent(
			"Product", "101", "ProductCreateAuctionEvent", "test-topic", "{}"
		);

		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		// kafkaTemplate.send()가 CompletableFuture를 반환하도록 모킹 (성공 시나리오)
		CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
		when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

		// when
		outboxUseCase.processAndPublish(eventId);

		// then
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT); // 상태 변경 확인

		// 카프카로 전달된 레코드의 상세 내용 검증 (헤더 포함 여부)
		verify(kafkaTemplate).send(argThat((ProducerRecord<String, Object> record) ->
			record.topic().equals("test-topic") &&
				record.headers().lastHeader("messageId") != null &&
				record.headers().lastHeader("__TypeId__") != null
		));
	}

	@Test
	@DisplayName("카프카 전송 실패 시 상태를 FAILED로 변경하고 재시도 횟수를 기록한다")
	void should_MarkAsFailed_When_PublishFails() throws Exception {
		// given
		Long eventId = 1L;
		OutboxEvent event = OutboxEvent.createOutboxEvent(
			"Product", "101", "ProductCreateAuctionEvent", "test-topic", "{}"
		);

		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		// 전송 실패 시나리오 모킹 (예외 발생)
		CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
		future.completeExceptionally(new RuntimeException("Kafka Broker Down"));
		when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

		// when
		outboxUseCase.processAndPublish(eventId);

		// then
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED); // 실패 상태 확인
		assertThat(event.getRetryCount()).isGreaterThan(0); // 재시도 횟수 증가 확인
		assertThat(event.getLastErrorMessage()).contains("Kafka Broker Down"); // 에러 메시지 저장 확인
	}

	@Test
	@DisplayName("존재하지 않는 이벤트 ID로 전송 시도 시 OUTBOX_NOT_FOUND 예외를 던진다")
	void should_ThrowException_When_EventIdNotFound() {
		// given
		Long invalidId = 999L;
		when(outboxEventRepository.findById(invalidId)).thenReturn(Optional.empty());

		// when & then
		CustomException exception = assertThrows(CustomException.class, () ->
			outboxUseCase.processAndPublish(invalidId)
		);
		assertThat(exception.getErrorType()).isEqualTo(ErrorType.OUTBOX_NOT_FOUND);
	}

	@Test
	@DisplayName("카프카 전송 중 타임아웃 발생 시 FAILED 상태로 변경된다")
	void should_MarkAsFailed_When_KafkaSendTimesOut() throws Exception {
		// given
		Long eventId = 1L;
		OutboxEvent event = OutboxEvent.createOutboxEvent("Test", "1", "Event", "topic", "{}");
		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		// KafkaTemplate이 대기하다가 TimeoutException을 던지도록 모킹
		CompletableFuture<SendResult<String, Object>> future = mock(CompletableFuture.class);
		when(future.get(anyLong(), any())).thenThrow(new TimeoutException("Kafka Timeout"));
		when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

		// when
		outboxUseCase.processAndPublish(eventId);

		// then
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
		assertThat(event.getLastErrorMessage()).contains("Kafka Timeout");
	}

	@Test
	@DisplayName("최대 재시도 횟수에 도달한 상태에서 실패하면 에러 메시지와 함께 FAILED 상태가 유지된다")
	void should_RemainFailed_When_MaxRetryIsReached() throws Exception {
		// given
		Long eventId = 1L;
		OutboxEvent event = OutboxEvent.createOutboxEvent("Test", "1", "Event", "topic", "{}");
		// 이미 4번 실패한 상태 (maxRetry가 5라고 가정)
		for(int i=0; i<4; i++) event.markFailed("Error", 5);

		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
		future.completeExceptionally(new RuntimeException("Last Attempt Failed"));
		when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

		// when
		outboxUseCase.processAndPublish(eventId);

		// then
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
		assertThat(event.getRetryCount()).isEqualTo(5); // 5회 도달 확인
	}

}
