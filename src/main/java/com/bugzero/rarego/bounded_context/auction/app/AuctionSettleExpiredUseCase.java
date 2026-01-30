package com.bugzero.rarego.bounded_context.auction.app;

import com.bugzero.rarego.bounded_context.auction.domain.Auction;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionOrder;
import com.bugzero.rarego.bounded_context.auction.domain.Bid;
import com.bugzero.rarego.bounded_context.auction.domain.event.AuctionFailedEvent;
import com.bugzero.rarego.bounded_context.auction.in.dto.AuctionAutoSettleResponseDto;
import com.bugzero.rarego.bounded_context.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.bounded_context.auction.out.AuctionRepository;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
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
public class AuctionSettleExpiredUseCase {

    private final AuctionSettlementSupport support;
    private final AuctionRepository auctionRepository;
    private final AuctionOrderRepository auctionOrderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuctionAutoSettleResponseDto execute() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> auctions = support.findExpiredAuctions(now);

        int success = 0;
        int fail = 0;
        List<AuctionAutoSettleResponseDto.SettlementDetail> details = new ArrayList<>();

        for (Auction auction : auctions) {
            try {
                if (support.hasBids(auction.getId())) {
                    handleSuccess(auction);
                    Bid winningBid = support.findWinningBid(auction.getId());
                    details.add(AuctionAutoSettleResponseDto.SettlementDetail.success(
                            auction.getId(),
                            winningBid.getBidderId()));
                    success++;
                } else {
                    handleFail(auction);
                    details.add(AuctionAutoSettleResponseDto.SettlementDetail.failed(auction.getId()));
                    fail++;
                }
            } catch (Exception e) {
                log.error("경매 낙찰 처리 실패 - auctionId: {}", auction.getId(), e);
                fail++;
            }
        }

        return AuctionAutoSettleResponseDto.from(now, auctions, success, fail, details);
    }

    private void handleFail(Auction auction) {
        auction.end();
        auctionRepository.save(auction);

        eventPublisher.publishEvent(
                new AuctionFailedEvent(
                        auction.getId(),
                        auction.getProductId()));
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