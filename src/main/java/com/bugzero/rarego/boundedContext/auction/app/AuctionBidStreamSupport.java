package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionBidEventDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionCompleteEventDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionConnectEventDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionHeartbeatEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * SSE Emitter 관리 및 이벤트 브로드캐스트 지원
 */
@Component
@Slf4j
public class AuctionBidStreamSupport {

    private static final Long DEFAULT_TIMEOUT = 60 * 60 * 1000L; // 1시간
    private static final Long HEARTBEAT_INTERVAL = 30L; // 30초

    private final ObjectMapper objectMapper;

    // auctionId별 구독자 목록 관리
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 하트비트 스케줄러
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    public AuctionBidStreamSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 하트비트 시작
        startHeartbeat();
    }

    /**
     * 경매 스트림 구독
     */
    public SseEmitter subscribe(Long auctionId, Integer currentPrice) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 경매방 번호가 없고 방이 처음 만들어졌다면 빈 리스트 생성해서 반환
        emitters.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        log.info("경매 {} 구독 시작 - 현재 구독자 수: {}", auctionId, emitters.get(auctionId).size());

        // 타임아웃/에러/완료 시 정리
        emitter.onTimeout(() -> {
            log.info("경매 {} 구독 타임아웃", auctionId);
            removeEmitter(auctionId, emitter);
        });

        emitter.onError((e) -> {
            log.error("경매 {} 구독 에러", auctionId, e);
            removeEmitter(auctionId, emitter);
        });

        emitter.onCompletion(() -> {
            log.info("경매 {} 구독 완료", auctionId);
            removeEmitter(auctionId, emitter);
        });

        // 최초 연결 이벤트 전송
        try {
            sendToEmitter(
                    emitter,
                    "connect",
                    System.currentTimeMillis() + "_init",
                    AuctionConnectEventDto.create(auctionId, currentPrice)
            );
        } catch (IOException e) {
            log.error("연결 이벤트 전송 실패", e);
            removeEmitter(auctionId, emitter);
        }

        return emitter;
    }

    /**
     * 특정 경매의 모든 구독자에게 입찰 이벤트 브로드캐스트
     */
    public void broadcastBid(
            Long auctionId,
            Integer bidAmount,
            String bidderName,
            LocalDateTime bidTime
    ) {
        AuctionBidEventDto event = AuctionBidEventDto.create(
                auctionId,
                bidAmount,
                bidderName,
                bidTime
        );

        broadcast(
                auctionId,
                "bid",
                System.currentTimeMillis() + "_" + auctionId,
                event
        );
    }

    /**
     * 경매 종료 이벤트 브로드캐스트
     */
    public void broadcastAuctionEnded(
            Long auctionId,
            Integer finalPrice,
            String winnerName
    ) {
        AuctionCompleteEventDto event = AuctionCompleteEventDto.create(
                auctionId,
                finalPrice,
                winnerName
        );

        broadcast(
                auctionId,
                "end",
                System.currentTimeMillis() + "_end",
                event
        );

        // 경매 종료 후 모든 연결 정리
        closeAllConnections(auctionId);
    }


    /**
     * 애플리케이션 종료 시 하트비트 스케줄러를 안전하게 종료
     */
    @PreDestroy
    public void stopHeartbeat() {
        log.info("SSE 하트비트 스케줄러 종료 시작...");
        heartbeatScheduler.shutdown();
        try {
            if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 현재 구독자 수 조회 (모니터링용)
     */
    public int getTotalSubscribers() {
        return emitters.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();
    }

    /**
     * 특정 경매 구독자 수 조회
     */
    public int getAuctionSubscribers(Long auctionId) {
        CopyOnWriteArrayList<SseEmitter> auctionEmitters = emitters.get(auctionId);
        return auctionEmitters != null ? auctionEmitters.size() : 0;
    }

    // helper method

    /**
     * 브로드캐스트 공통 로직
     */
    private void broadcast(Long auctionId, String eventName, String eventId, Object data) {
        CopyOnWriteArrayList<SseEmitter> auctionEmitters = emitters.get(auctionId);

        if (auctionEmitters == null || auctionEmitters.isEmpty()) {
            log.debug("경매 {}의 구독자가 없습니다.", auctionId);
            return;
        }

        log.info("경매 {} 이벤트 브로드캐스트 - {} 구독자", auctionId, auctionEmitters.size());

        auctionEmitters.forEach(emitter -> {
            try {
                sendToEmitter(emitter, eventName, eventId, data);
            } catch (IOException e) {
                log.error("이벤트 전송 실패", e);
                removeEmitter(auctionId, emitter);
            }
        });
    }

    /**
     * 개별 Emitter에 이벤트 전송 (String 데이터 대응)
     */
    private void sendToEmitter(SseEmitter emitter, String eventName, String eventId, Object data)
            throws IOException {

        // 데이터가 이미 String(JSON)이면 그대로 쓰고, 아니면 직렬화 수행
        String jsonData = (data instanceof String) ? (String) data : objectMapper.writeValueAsString(data);

        emitter.send(SseEmitter.event()
                .name(eventName)
                .id(eventId)
                .data(jsonData)
        );
    }

    /**
     * Emitter 제거
     */
    private void removeEmitter(Long auctionId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> auctionEmitters = emitters.get(auctionId);
        if (auctionEmitters != null) {
            auctionEmitters.remove(emitter);

            // 구독자가 없으면 목록 자체를 제거
            if (auctionEmitters.isEmpty()) {
                emitters.remove(auctionId);
                log.info("경매 {} 구독자 목록 제거", auctionId);
            }
        }
    }

    /**
     * 특정 경매의 모든 연결 종료
     */
    private void closeAllConnections(Long auctionId) {
        CopyOnWriteArrayList<SseEmitter> auctionEmitters = emitters.remove(auctionId);
        if (auctionEmitters != null) {
            auctionEmitters.forEach(SseEmitter::complete);
            log.info("경매 {} 모든 연결 종료 - {}건", auctionId, auctionEmitters.size());
        }
    }

    /**
     * 하트비트 (30초마다 PING 전송)
     */
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                // 루프 밖에서 한 번만 JSON으로 직렬화
                String heartbeatJson = objectMapper.writeValueAsString(AuctionHeartbeatEventDto.create());

                emitters.forEach((auctionId, auctionEmitters) -> {
                    auctionEmitters.forEach(emitter -> {
                        try {
                            sendToEmitter(
                                    emitter,
                                    "ping",
                                    System.currentTimeMillis() + "_ping",
                                    heartbeatJson
                            );
                        } catch (IOException e) {
                            log.debug("하트비트 전송 실패 (연결 끊김)", e);
                            removeEmitter(auctionId, emitter);
                        }
                    });
                });
            } catch (Exception e) {
                log.error("하트비트 직렬화 실패", e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);

        log.info("SSE 하트비트 스케줄러 시작 - {}초 간격", HEARTBEAT_INTERVAL);
    }

}