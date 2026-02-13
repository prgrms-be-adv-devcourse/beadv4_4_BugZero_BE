package com.bugzero.rarego.out;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionRelistedEvent;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionEventKafkaBridge 테스트")
class AuctionEventKafkaBridgeTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AuctionEventKafkaBridge auctionEventKafkaBridge;

    private AuctionEndedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new AuctionEndedEvent(123L, 456L, 50000, 789L);
    }

    @Test
    @DisplayName("경매 종료 이벤트를 Kafka로 정상 전송한다")
    void sendAuctionEndedToKafka_Success() {
        // when
        auctionEventKafkaBridge.sendAuctionEndedToKafka(testEvent);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate, times(1)).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo("auction-ended");
        assertThat(keyCaptor.getValue()).isEqualTo("123");
        assertThat(eventCaptor.getValue()).isEqualTo(testEvent);
    }

    @Test
    @DisplayName("auctionId를 Key로 사용하여 메시지 순서를 보장한다")
    void sendAuctionEndedToKafka_UsesAuctionIdAsKey() {
        // given
        Long expectedAuctionId = 999L;
        AuctionEndedEvent event = new AuctionEndedEvent(expectedAuctionId, 100L, 30000, 200L);

        // when
        auctionEventKafkaBridge.sendAuctionEndedToKafka(event);

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                eq("auction-ended"),
                keyCaptor.capture(),
                eq(event)
        );

        assertThat(keyCaptor.getValue()).isEqualTo(String.valueOf(expectedAuctionId));
    }

    @Test
    @DisplayName("유찰된 경매 이벤트를 전송한다 (winnerId와 finalPrice가 null)")
    void sendAuctionEndedToKafka_UnsoldAuction() {
        // given
        AuctionEndedEvent unsoldEvent = new AuctionEndedEvent(123L, null, null, 789L);

        // when
        auctionEventKafkaBridge.sendAuctionEndedToKafka(unsoldEvent);

        // then
        ArgumentCaptor<AuctionEndedEvent> eventCaptor = ArgumentCaptor.forClass(AuctionEndedEvent.class);
        verify(kafkaTemplate).send(
                eq("auction-ended"),
                eq("123"),
                eventCaptor.capture()
        );

        AuctionEndedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.auctionId()).isEqualTo(123L);
        assertThat(capturedEvent.winnerId()).isNull();
        assertThat(capturedEvent.finalPrice()).isNull();
        assertThat(capturedEvent.productId()).isEqualTo(789L);
    }

    @Test
    @DisplayName("여러 이벤트를 순차적으로 전송한다")
    void sendAuctionEndedToKafka_MultipleEvents() {
        // given
        AuctionEndedEvent event1 = new AuctionEndedEvent(1L, 10L, 10000, 100L);
        AuctionEndedEvent event2 = new AuctionEndedEvent(2L, 20L, 20000, 200L);
        AuctionEndedEvent event3 = new AuctionEndedEvent(3L, null, null, 300L); // 유찰

        // when
        auctionEventKafkaBridge.sendAuctionEndedToKafka(event1);
        auctionEventKafkaBridge.sendAuctionEndedToKafka(event2);
        auctionEventKafkaBridge.sendAuctionEndedToKafka(event3);

        // then
        verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
        verify(kafkaTemplate).send("auction-ended", "1", event1);
        verify(kafkaTemplate).send("auction-ended", "2", event2);
        verify(kafkaTemplate).send("auction-ended", "3", event3);
    }

    @Test
    @DisplayName("이벤트의 모든 필드가 올바르게 전송된다")
    void sendAuctionEndedToKafka_AllFieldsPreserved() {
        // given
        AuctionEndedEvent fullEvent = new AuctionEndedEvent(555L, 777L, 99999, 888L);

        // when
        auctionEventKafkaBridge.sendAuctionEndedToKafka(fullEvent);

        // then
        ArgumentCaptor<AuctionEndedEvent> eventCaptor = ArgumentCaptor.forClass(AuctionEndedEvent.class);
        verify(kafkaTemplate).send(
                eq("auction-ended"),
                anyString(),
                eventCaptor.capture()
        );

        AuctionEndedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.auctionId()).isEqualTo(555L);
        assertThat(capturedEvent.winnerId()).isEqualTo(777L);
        assertThat(capturedEvent.finalPrice()).isEqualTo(99999);
        assertThat(capturedEvent.productId()).isEqualTo(888L);
    }

    @Test
    @DisplayName("경매 재생성 이벤트를 Kafka로 정상 전송한다")
    void sendAuctionRelistedToKafka_Success() {
        // given
        AuctionRelistedEvent relistedEvent = new AuctionRelistedEvent(
            999L,                   // productId
            2001L,                  // newAuctionId
            150000,                 // startPrice
            LocalDateTime.now()     // startedAt
        );

        // when
        auctionEventKafkaBridge.sendAuctionRelistedToKafka(relistedEvent);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate, times(1)).send(
            topicCaptor.capture(),
            keyCaptor.capture(),
            eventCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo("auction-relisted");
        assertThat(keyCaptor.getValue()).isEqualTo("2001"); // newAuctionId가 Key가 되어야 함
        assertThat(eventCaptor.getValue()).isEqualTo(relistedEvent);
    }

    @Test
    @DisplayName("재생성 이벤트의 newAuctionId를 Key로 사용하여 순서를 보장한다")
    void sendAuctionRelistedToKafka_UsesNewAuctionIdAsKey() {
        // given
        Long expectedNewAuctionId = 7777L;
        AuctionRelistedEvent event = new AuctionRelistedEvent(
            123L,
            expectedNewAuctionId,
            10000,
            LocalDateTime.now()
        );

        // when
        auctionEventKafkaBridge.sendAuctionRelistedToKafka(event);

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
            eq("auction-relisted"), // 토픽명 확인
            keyCaptor.capture(),
            eq(event)
        );

        assertThat(keyCaptor.getValue()).isEqualTo(String.valueOf(expectedNewAuctionId));
    }
}
