package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionFailedEvent;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionSettleOneUseCase {

    private final AuctionSettlementSupport support;
    private final AuctionRepository auctionRepository;
    private final AuctionOrderRepository auctionOrderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

        // 상태 검증
        if (auction.getStatus() == AuctionStatus.ENDED) {
            log.warn("경매 {}는 이미 종료되었습니다.", auctionId);
            return;
        }

        if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
            log.warn("경매 {}는 진행 중 상태가 아닙니다. 현재 상태: {}", auctionId, auction.getStatus());
            throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS);
        }

        // 시간 검증
        if (auction.getEndTime().isAfter(LocalDateTime.now())) {
            log.warn("경매 {}는 아직 종료 시간이 아닙니다. 종료 시간: {}", auctionId, auction.getEndTime());
            throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS);
        }

        // 입찰 여부에 따라 처리
        if (support.hasBids(auctionId)) {
            handleSuccess(auction);
            log.info("경매 {} 낙찰 처리 완료", auctionId);
        } else {
            handleFail(auction);
            log.info("경매 {} 유찰 처리 완료", auctionId);
        }
    }

    private void handleFail(Auction auction) {
        auction.end();
        auctionRepository.save(auction);

        eventPublisher.publishEvent(
                new AuctionFailedEvent(
                        auction.getId(),
                        auction.getProductId()
                )
        );
    }

    private void handleSuccess(Auction auction) {
        Bid winningBid = support.findWinningBid(auction.getId());

        auction.end();
        auctionRepository.save(auction);

        auctionOrderRepository.save(
                AuctionOrder.builder()
                        .auctionId(auction.getId())
                        .sellerId(auction.getSellerId())
                        .bidderId(winningBid.getBidderId())
                        .finalPrice(winningBid.getBidAmount())
                        .build()
        );

        eventPublisher.publishEvent(
                new AuctionEndedEvent(
                        auction.getId(),
                        winningBid.getBidderId(),
                        winningBid.getBidAmount(),
                        auction.getProductId()
                )
        );
    }
}