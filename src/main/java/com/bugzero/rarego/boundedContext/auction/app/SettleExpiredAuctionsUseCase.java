package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettleExpiredAuctionsUseCase {

    private final AuctionSettlementSupport support;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuctionAutoResponseDto execute() {

        LocalDateTime now = LocalDateTime.now();
        List<Auction> auctions = support.findExpiredAuctions(now);

        int success = 0;
        int fail = 0;
        List<AuctionAutoResponseDto.SettlementDetail> details = new ArrayList<>();

        for (Auction auction : auctions) {
            try {
                if (support.hasBids(auction.getId())) {
                    handleSuccess(auction);
                    Bid winningBid = support.findWinningBid(auction.getId());
                    details.add(AuctionAutoResponseDto.SettlementDetail.success(
                            auction.getId(),
                            winningBid.getBidderId()
                    ));
                    success++;
                } else {
                    handleFail(auction);
                    details.add(AuctionAutoResponseDto.SettlementDetail.failed(auction.getId()));
                    fail++;
                }
            } catch (Exception e) {
                log.error("경매 낙찰 처리 실패 - auctionId: {}", auction.getId(), e);
                fail++;
            }
        }

        return AuctionAutoResponseDto.from(
                now,
                auctions,
                success,
                fail,
                details
        );
    }

    private void handleFail(Auction auction) {
        auction.end();

        support.saveAuction(auction);

        eventPublisher.publishEvent(
                AuctionFailedEvent.builder()
                        .auctionId(auction.getId())
                        .productId(auction.getProductId())
                        .build()
        );
    }


    private void handleSuccess(Auction auction) {
        Bid winningBid = support.findWinningBid(auction.getId());

        auction.end();
        support.saveAuction(auction);

        support.saveOrder(
                AuctionOrder.builder()
                        .auctionId(auction.getId())
                        .bidderId(winningBid.getBidderId())
                        .finalPrice(winningBid.getBidAmount())
                        .build()
        );

        eventPublisher.publishEvent(
                AuctionEndedEvent.builder()
                        .auctionId(auction.getId())
                        .winnerId(winningBid.getBidderId())
                        .finalPrice(winningBid.getBidAmount())
                        .productId(auction.getProductId())
                        .build()
        );
    }
}