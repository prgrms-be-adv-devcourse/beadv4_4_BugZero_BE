package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionAutoSettleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionSettleAuctionFacade {

    private final AuctionSettleExpiredUseCase auctionSettleExpiredUseCase;
    private final AuctionSettleOneUseCase auctionSettleOneUseCase;

    /**
     * 만료된 모든 경매 일괄 정산 (수동 호출용)
     * 추후에 사용하지 않는다면 삭제 예정
     */
    public AuctionAutoSettleResponseDto settle() {
        return auctionSettleExpiredUseCase.execute();
    }

    /**
     * 특정 경매 하나만 정산 (동적 스케줄링용)
     */
    public void settleOne(Long auctionId) {
        auctionSettleOneUseCase.execute(auctionId);
    }
}