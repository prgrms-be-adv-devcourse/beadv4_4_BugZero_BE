package com.bugzero.rarego.bounded_context.auction.app;

import com.bugzero.rarego.bounded_context.auction.domain.Auction;
import com.bugzero.rarego.bounded_context.auction.domain.Bid;
import com.bugzero.rarego.bounded_context.auction.out.AuctionRepository;
import com.bugzero.rarego.bounded_context.auction.out.BidRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionSettlementSupport {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

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
        return bidRepository.findTopByAuctionIdOrderByBidAmountDescBidTimeAsc(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.BID_NOT_FOUND));
    }
}