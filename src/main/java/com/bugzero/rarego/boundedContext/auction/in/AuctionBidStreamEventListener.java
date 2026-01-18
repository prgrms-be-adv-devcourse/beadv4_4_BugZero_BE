package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionBidStreamSupport;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionBidCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionFailedEvent;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 입찰/경매 종료 이벤트를 SSE 스트림으로 브로드캐스트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionBidStreamEventListener {

    private final AuctionBidStreamSupport streamSupport;
    private final AuctionMemberRepository memberRepository;

    /**
     * 입찰 생성 이벤트 → SSE 브로드캐스트
     * 트랜잭션 커밋 후 실행되어야 안전함
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBidCreated(AuctionBidCreatedEvent event) {
        try {
            log.info("입찰 이벤트 수신 - auctionId: {}, bidderId: {}",
                    event.auctionId(), event.bidderId());

            // 입찰자 닉네임 조회
            String bidderName = memberRepository.findById(event.bidderId())
                    .map(AuctionMember::getNickname)
                    .orElse("익명");

            // SSE 브로드캐스트
            streamSupport.broadcastBid(
                    event.auctionId(),
                    event.bidAmount(),
                    bidderName,
                    event.bidTime()
            );

        } catch (Exception e) {
            log.error("입찰 이벤트 브로드캐스트 실패 - auctionId: {}", event.auctionId(), e);
            // SSE 전송 실패는 비즈니스 로직에 영향 없으므로 예외를 던지지 않음
        }
    }

    /**
     * 경매 낙찰 이벤트 → SSE 브로드캐스트 (경매 종료)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionEnded(AuctionEndedEvent event) {
        try {
            log.info("경매 종료 이벤트 수신 - auctionId: {}, winnerId: {}",
                    event.auctionId(), event.winnerId());

            // 낙찰자 닉네임 조회
            String winnerName = event.winnerId() != null
                    ? memberRepository.findById(event.winnerId())
                    .map(AuctionMember::getNickname)
                    .orElse("익명")
                    : null;

            // SSE 브로드캐스트
            streamSupport.broadcastAuctionEnded(
                    event.auctionId(),
                    event.finalPrice(),
                    winnerName
            );

        } catch (Exception e) {
            log.error("경매 종료 이벤트 브로드캐스트 실패 - auctionId: {}", event.auctionId(), e);
        }
    }

    /**
     * 경매 유찰 이벤트 → SSE 브로드캐스트 (경매 종료, 낙찰자 없음)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionFailed(AuctionFailedEvent event) {
        try {
            log.info("경매 유찰 이벤트 수신 - auctionId: {}", event.auctionId());

            // SSE 브로드캐스트 (낙찰자 없음)
            streamSupport.broadcastAuctionEnded(
                    event.auctionId(),
                    0,  // 유찰이므로 최종가 0
                    null  // 낙찰자 없음
            );

        } catch (Exception e) {
            log.error("경매 유찰 이벤트 브로드캐스트 실패 - auctionId: {}", event.auctionId(), e);
        }
    }
}
