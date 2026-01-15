package com.bugzero.rarego.boundedContext.auction.app;


import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionBidStreamEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
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

        // 구독자 목록에 추가
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
                    AuctionBidStreamEventDto.connect(auctionId, currentPrice)
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
            java.time.LocalDateTime bidTime
    ) {
        AuctionBidStreamEventDto event = AuctionBidStreamEventDto.bid(
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
        AuctionBidStreamEventDto event = AuctionBidStreamEventDto.auctionEnded(
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
     * 브로드캐스트 공통 로직
     */
    private void broadcast(Long auctionId, String eventName, String eventId, AuctionBidStreamEventDto data) {
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
     * 개별 Emitter에 이벤트 전송
     */
    private void sendToEmitter(SseEmitter emitter, String eventName, String eventId, Object data)
            throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .id(eventId)
                .data(objectMapper.writeValueAsString(data))
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
            emitters.forEach((auctionId, auctionEmitters) -> {
                auctionEmitters.forEach(emitter -> {
                    try {
                        sendToEmitter(
                                emitter,
                                "ping",
                                System.currentTimeMillis() + "_ping",
                                AuctionBidStreamEventDto.ping()
                        );
                    } catch (IOException e) {
                        log.debug("하트비트 전송 실패 (연결 끊김)", e);
                        removeEmitter(auctionId, emitter);
                    }
                });
            });
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);

        log.info("SSE 하트비트 스케줄러 시작 - {}초 간격", HEARTBEAT_INTERVAL);
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
}
