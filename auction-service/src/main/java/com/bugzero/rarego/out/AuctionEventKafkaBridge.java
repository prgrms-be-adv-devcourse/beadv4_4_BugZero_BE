package com.bugzero.rarego.out;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionRelistedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionStartedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AuctionEndedEvent를 Kafka로 중계하는 브릿지
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventKafkaBridge {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_AUCTION_ENDED = "auction-ended";
    private static final String TOPIC_AUCTION_STARTED = "auction-started";
    private static final String TOPIC_AUCTION_RELISTED = "auction-relisted";

    /**
     * 경매 종료 이벤트 중계
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAuctionEndedToKafka(AuctionEndedEvent event) {
        log.info("Bridge: Kafka로 경매 종료 이벤트 전송 [Topic: {}, AuctionId: {}]",
                TOPIC_AUCTION_ENDED, event.auctionId());

        String key = String.valueOf(event.auctionId());
        kafkaTemplate.send(TOPIC_AUCTION_ENDED, key, event);
    }

    /**
     * 경매 시작 이벤트 중계
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAuctionStartedToKafka(AuctionStartedEvent event) {
        log.info("Bridge: Kafka로 경매 시작 이벤트 전송 [Topic: {}, AuctionId: {}]",
                TOPIC_AUCTION_STARTED, event.auctionId());

        String key = String.valueOf(event.auctionId());

        kafkaTemplate.send(TOPIC_AUCTION_STARTED, key, event);
    }

    /**
     * 경매 재등록 이벤트 중계
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAuctionRelistedToKafka(AuctionRelistedEvent event) {
        log.info("Bridge: Kafka로 경매 재등록 이벤트 전송 [Topic: {}, ProductId: {}, NewAuctionId: {}]",
                TOPIC_AUCTION_RELISTED, event.productId(), event.newAuctionId());

        String key = String.valueOf(event.newAuctionId());
        kafkaTemplate.send(TOPIC_AUCTION_RELISTED, key, event);
    }
}
