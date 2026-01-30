package com.bugzero.rarego.bounded_context.auction.app;

import com.bugzero.rarego.bounded_context.auction.in.dto.AuctionBidEventDto;
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
        CountDownLatch latch = new CountDownLatch(1);

        // when
        SseEmitter emitter = support.subscribe(auctionId, currentPrice);
        emitter.onCompletion(latch::countDown);

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
        support.subscribe(auctionId, 100_000);
        support.subscribe(auctionId, 100_000);

        assertThat(support.getAuctionSubscribers(auctionId)).isEqualTo(2);

        // when
        support.broadcastAuctionEnded(auctionId, 150_000, "김철수");

        // then - 모든 연결이 종료되어야 함
        Thread.sleep(100); // 비동기 처리 대기
        assertThat(support.getAuctionSubscribers(auctionId)).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 경매 동시 구독 및 전체 구독자 수 확인")
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
    @DisplayName("스프링 컨테이너 종료 시(PreDestroy) 하트비트 스케줄러가 안전하게 정지되어야 한다")
    void stopHeartbeat_ShouldShutdownScheduler() throws Exception {
        // given: Reflection을 사용하여 private 필드인 heartbeatScheduler에 접근
        Field field = AuctionBidStreamSupport.class.getDeclaredField("heartbeatScheduler");
        field.setAccessible(true);
        ScheduledExecutorService scheduler = (ScheduledExecutorService) field.get(support);

        // 초기 상태 확인 (실행 중이어야 함)
        assertThat(scheduler.isShutdown()).isFalse();

        // when: 종료 메서드 호출
        support.stopHeartbeat();

        // then: 스케줄러가 shutdown 상태인지 검증
        assertTrue(scheduler.isShutdown(), "스케줄러가 shutdown 상태여야 합니다.");
    }

    @Test
    @DisplayName("이름 마스킹 테스트")
    void maskName() {
        // given
        AuctionBidEventDto event = AuctionBidEventDto.create(
                1L,
                100_000,
                "김철수",
                LocalDateTime.now()
        );

        // then
        assertThat(event.getMaskedBidderName()).isEqualTo("김*수");

        // given
        AuctionBidEventDto event2 = AuctionBidEventDto.create(
                1L,
                100_000,
                "legoKing",
                LocalDateTime.now()
        );

        // then
        assertThat(event2.getMaskedBidderName()).contains("*");
        assertThat(event2.getMaskedBidderName()).startsWith("le");
    }
}