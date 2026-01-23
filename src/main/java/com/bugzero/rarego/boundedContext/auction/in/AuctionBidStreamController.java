package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionBidStreamSupport;
import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 경매 실시간 입찰 스트림 API
 */
@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Slf4j
public class AuctionBidStreamController {

    private final AuctionBidStreamSupport streamSupport;
    private final AuctionRepository auctionRepository;

    private static final int MAX_SUBSCRIBERS_PER_AUCTION = 1000; // 경매당 최대 구독자 수

    /**
     * 경매 실시간 입찰 이벤트 구독
     *
     * @param auctionId 경매 ID
     * @return SSE Emitter
     */
    // TODO: 비즈니스 로직이 많아서 리팩토링 필요
    @GetMapping(value = "/{auctionId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long auctionId) {
        log.info("경매 {} 스트림 구독 요청", auctionId);

        // 1. 경매 존재 여부 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

        // 2. 구독자 수 제한 확인
        int currentSubscribers = streamSupport.getAuctionSubscribers(auctionId);
        if (currentSubscribers >= MAX_SUBSCRIBERS_PER_AUCTION) {
            log.warn("경매 {} 구독자 수 한도 초과 - 현재: {}", auctionId, currentSubscribers);
            throw new CustomException(ErrorType.SERVICE_SUBSCRIBER_LIMIT_EXCEEDED);
        }

        // 3. 현재가 조회 (최신 입찰가 또는 시작가)
        Integer currentPrice = auction.getCurrentPrice() != null
                ? auction.getCurrentPrice()
                : auction.getStartPrice();

        // 4. SSE 구독 시작
        return streamSupport.subscribe(auctionId, currentPrice);
    }

    /**
     * 모니터링용 - 전체 구독자 수 조회
     */
    @GetMapping("/subscribers/count")
    public int getTotalSubscribers() {
        return streamSupport.getTotalSubscribers();
    }

    /**
     * 모니터링용 - 특정 경매 구독자 수 조회
     */
    @GetMapping("/{auctionId}/subscribers/count")
    public int getAuctionSubscribers(@PathVariable Long auctionId) {
        return streamSupport.getAuctionSubscribers(auctionId);
    }
}