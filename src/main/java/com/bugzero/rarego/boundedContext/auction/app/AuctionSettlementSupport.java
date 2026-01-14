package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// (조회 + 저장 담당)
@Component
@RequiredArgsConstructor
public class AuctionSettlementSupport {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionOrderRepository auctionOrderRepository;

    private static final int BATCH_SIZE = 100;

    public List<Auction> findExpiredAuctions(LocalDateTime now) {
        return auctionRepository.findExpiredInProgressAuctionsWithLock(
                now, PageRequest.of(0, BATCH_SIZE)
        );
    }

    public boolean hasBids(Long auctionId) {
        return bidRepository.existsByAuctionId(auctionId);
    }

    public Bid findWinningBid(Long auctionId) {
        return bidRepository.findTopByAuctionId(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.HIGHEST_BID_NOT_FOUND));
    }

    public void saveAuction(Auction auction) {
        auctionRepository.save(auction);
    }

    public void saveOrder(AuctionOrder order) {
        auctionOrderRepository.save(order);
    }
}
