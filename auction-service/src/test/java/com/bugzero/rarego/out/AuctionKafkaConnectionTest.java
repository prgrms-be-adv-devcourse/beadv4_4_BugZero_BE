package com.bugzero.rarego.out;

import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionRelistedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionStartedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

@SpringBootTest(classes = {KafkaAutoConfiguration.class})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:29092",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
class AuctionKafkaConnectionTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("로컬 카프카로 실제 경매 종료 메시지 전송 테스트")
    void testAuctionEndedRealSend() {
        // 1. 보낼 데이터 생성
        AuctionEndedEvent event = new AuctionEndedEvent(999L, 777L, 150000, 888L);

        // 2. 실제 카프카로 전송 (토픽명: auction-ended)
        // 전송 후 get()을 호출하여 성공할 때까지 기다림
        try {
            kafkaTemplate.send("auction-ended", "999", event).get();
            System.out.println("카프카 경매 종료 메시지 전송 성공!");
        } catch (Exception e) {
            System.err.println("전송 실패: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("로컬 카프카로 실제 재등록 메시지 전송 테스트")
    void testRelistRealSend() {
        // 1. 보낼 데이터 생성
        AuctionRelistedEvent event = new AuctionRelistedEvent(
                999L,                    // productId
                1001L,                   // newAuctionId
                120000,                  // startPrice
                LocalDateTime.now()      // startedAt
        );

        // 2. 실제 카프카로 전송 (토픽명: auction-relisted)
        // 전송 후 get()을 호출하여 성공할 때까지 기다림
        try {
            kafkaTemplate.send("auction-relisted", "999", event).get();
            System.out.println("카프카 재등록 메시지 전송 성공!");
        } catch (Exception e) {
            System.err.println("전송 실패: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("로컬 카프카로 실제 경매 시작 메시지 전송 테스트")
    void testAuctionStartedRealSend() {
        // 1. 보낼 데이터 생성
        AuctionStartedEvent event = new AuctionStartedEvent(
                777L,                    // auctionId
                999L,                    // productId
                LocalDateTime.now()      // startedAt
        );

        // 2. 실제 카프카로 전송 (토픽명: auction-started)
        // 전송 후 get()을 호출하여 성공할 때까지 기다림
        try {
            kafkaTemplate.send("auction-started", "777", event).get();
            System.out.println("카프카 경매 시작 메시지 전송 성공!");
        } catch (Exception e) {
            System.err.println("전송 실패: " + e.getMessage());
        }
    }
}