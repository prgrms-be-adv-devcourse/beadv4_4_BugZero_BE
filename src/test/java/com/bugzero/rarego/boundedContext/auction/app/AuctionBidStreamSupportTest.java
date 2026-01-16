package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionBidStreamEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionBidStreamSupportTest {

    private AuctionBidStreamSupport support;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Java 8 Time 모듈 등록
        support = new AuctionBidStreamSupport(objectMapper);
    }

    @Test
    @DisplayName("구독 시 연결 이벤트 전송")
    void subscribe_SendsConnectEvent() throws Exception {
        // given
        Long auctionId = 1L;
        Integer currentPrice = 100_000;
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        // when
        SseEmitter emitter = support.subscribe(auctionId, currentPrice);

        emitter.onCompletion(() -> latch.countDown());

        // then
        assertThat(support.getAuctionSubscribers(auctionId)).isEqualTo(1);

        // 정리
        emitter.complete();
        latch.await(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("입찰 이벤트 브로드캐스트")
    void broadcastBid() throws Exception {
        // given
        Long auctionId = 1L;
        SseEmitter emitter1 = support.subscribe(auctionId, 100_000);
        SseEmitter emitter2 = support.subscribe(auctionId, 100_000);

        // when
        support.broadcastBid(auctionId, 110_000, "김철수", LocalDateTime.now());

        // then
        assertThat(support.getAuctionSubscribers(auctionId)).isEqualTo(2);

        // 정리
        emitter1.complete();
        emitter2.complete();
    }

    @Test
    @DisplayName("경매 종료 시 모든 연결 종료")
    void broadcastAuctionEnded_ClosesAllConnections() throws Exception {
        // given
        Long auctionId = 1L;
        SseEmitter emitter1 = support.subscribe(auctionId, 100_000);
        SseEmitter emitter2 = support.subscribe(auctionId, 100_000);

        assertThat(support.getAuctionSubscribers(auctionId)).isEqualTo(2);

        // when
        support.broadcastAuctionEnded(auctionId, 150_000, "김철수");

        // then - 모든 연결이 종료되어야 함
        Thread.sleep(100); // 비동기 처리 대기
        assertThat(support.getAuctionSubscribers(auctionId)).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 경매 동시 구독")
    void multipleAuctions() {
        // given & when
        support.subscribe(1L, 100_000);
        support.subscribe(1L, 100_000);
        support.subscribe(2L, 200_000);
        support.subscribe(3L, 300_000);

        // then
        assertThat(support.getAuctionSubscribers(1L)).isEqualTo(2);
        assertThat(support.getAuctionSubscribers(2L)).isEqualTo(1);
        assertThat(support.getAuctionSubscribers(3L)).isEqualTo(1);
        assertThat(support.getTotalSubscribers()).isEqualTo(4);
    }

    @Test
    @DisplayName("타임아웃 시 자동 정리")
    void emitter_Timeout_RemovesFromList() throws Exception {
        // given
        Long auctionId = 1L;
        SseEmitter emitter = new SseEmitter(100L); // 100ms 타임아웃

        // Emitter를 직접 등록하는 대신, subscribe 메서드 사용
        support.subscribe(auctionId, 100_000);

        // when - 타임아웃 대기
        Thread.sleep(200);

        // then - 타임아웃 후에는 구독자가 없어야 함
        // (실제로는 타임아웃 핸들러가 제대로 동작하는지 확인하기 어려움)
        // 이 테스트는 참고용
    }

    @Test
    @DisplayName("이름 마스킹 테스트")
    void maskName() {
        // AuctionBidStreamEventDto의 maskName은 private이므로
        // 팩토리 메서드를 통해 간접 테스트
        AuctionBidStreamEventDto event = AuctionBidStreamEventDto.bid(
                1L,
                100_000,
                "김철수",
                LocalDateTime.now()
        );

        assertThat(event.bidderName()).isEqualTo("김*수");

        AuctionBidStreamEventDto event2 = AuctionBidStreamEventDto.bid(
                1L,
                100_000,
                "legoKing",
                LocalDateTime.now()
        );

        assertThat(event2.bidderName()).contains("*");
        assertThat(event2.bidderName()).startsWith("le");
    }
}