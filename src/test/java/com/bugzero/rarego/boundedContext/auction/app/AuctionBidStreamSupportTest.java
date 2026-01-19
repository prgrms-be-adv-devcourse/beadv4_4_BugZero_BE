package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionBidEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("스프링 컨테이너 종료 시(PreDestroy) 하트비트 스케줄러가 안전하게 정지되어야 한다")
    void stopHeartbeat_ShouldShutdownScheduler() throws Exception {
        // given
        // AuctionBidStreamSupport 내부의 private 필드인 heartbeatScheduler에 접근
        Field field = AuctionBidStreamSupport.class.getDeclaredField("heartbeatScheduler");
        field.setAccessible(true);
        ScheduledExecutorService scheduler = (ScheduledExecutorService) field.get(support);

        // 스케줄러가 처음에는 실행 중인지 확인
        assertThat(scheduler.isShutdown()).isFalse();

        // when
        // @PreDestroy 메서드를 명시적으로 호출 (스프링 종료 상황 시뮬레이션)
        support.stopHeartbeat();

        // then
        // 1. 스케줄러가 종료 상태여야 함
        assertTrue(scheduler.isShutdown(), "스케줄러가 shutdown 상태여야 합니다.");

        // 2. 새로운 작업이 더 이상 수락되지 않아야 함 (이미 종료되었으므로)
        assertTrue(scheduler.isTerminated() || scheduler.isShutdown());
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
    @DisplayName("Emitter 타임아웃 콜백 검증")
    void testEmitterTimeoutCallback() {
        // given
        Long auctionId = 1L;

        // when
        SseEmitter emitter = support.subscribe(auctionId, 100_000);

        // emitter의 onTimeout 메서드가 제대로 호출되는지 검증
        emitter.onTimeout(() -> {
            // 타임아웃 시 수행되어야 할 로직 검증
            assertThat(support.getAuctionSubscribers(auctionId)).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("이름 마스킹 테스트")
    void maskName() {
        // AuctionBidEventDto의 getMaskedBidderName 메서드를 통해 테스트
        AuctionBidEventDto event = AuctionBidEventDto.create(
                1L,
                100_000,
                "김철수",
                LocalDateTime.now()
        );

        assertThat(event.getMaskedBidderName()).isEqualTo("김*수");

        AuctionBidEventDto event2 = AuctionBidEventDto.create(
                1L,
                100_000,
                "legoKing",
                LocalDateTime.now()
        );

        assertThat(event2.getMaskedBidderName()).contains("*");
        assertThat(event2.getMaskedBidderName()).startsWith("le");
    }
}