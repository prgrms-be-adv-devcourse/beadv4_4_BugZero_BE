package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SettleExpiredAuctionsUseCaseTest {

    @Mock
    AuctionSettlementSupport support;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    SettleExpiredAuctionsUseCase useCase;

    private Auction createExpiredAuction(Long id) {
        Auction auction = Auction.builder()
                .productId(100L)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().minusMinutes(1))
                .startPrice(10_000)
                .tickSize(1_000)
                .build();

        ReflectionTestUtils.setField(auction, "id", id);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);

        return auction;
    }

    private Bid createWinningBid() {
        Bid bid = mock(Bid.class);
        ReflectionTestUtils.setField(bid, "bidderId", 10L);
        ReflectionTestUtils.setField(bid, "bidAmount", 50_000);
        ReflectionTestUtils.setField(bid, "auctionId", 1L);
        return bid;
    }

    @Test
    void 입찰이_없으면_유찰_이벤트가_발생한다() {
        // given
        Auction auction = createExpiredAuction(1L);
        given(support.findExpiredAuctions(any())).willReturn(List.of(auction));
        given(support.hasBids(auction.getId())).willReturn(false);

        // when - Map 대신 DTO로 받음
        AuctionAutoResponseDto result = useCase.execute();

        // then
        verify(eventPublisher).publishEvent(any(AuctionFailedEvent.class));
        verify(support).saveAuction(auction);

        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    @Test
    void 입찰이_있으면_낙찰_처리되고_주문이_생성된다() {
        // given
        Auction auction = createExpiredAuction(1L);
        Bid bid = createWinningBid();
        given(support.findExpiredAuctions(any())).willReturn(List.of(auction));
        given(support.hasBids(auction.getId())).willReturn(true);
        given(support.findWinningBid(auction.getId())).willReturn(bid);

        // when
        AuctionAutoResponseDto result = useCase.execute();

        // then
        verify(eventPublisher).publishEvent(any(AuctionEndedEvent.class));
        verify(support).saveOrder(any(AuctionOrder.class));
        verify(support).saveAuction(auction);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(0);
    }

    @Test
    void 여러_경매가_섞여있으면_결과_카운트가_정확해야한다() {
        // given
        Auction successAuction = createExpiredAuction(1L);
        Auction failAuction = createExpiredAuction(2L);
        given(support.findExpiredAuctions(any())).willReturn(List.of(successAuction, failAuction));
        given(support.hasBids(successAuction.getId())).willReturn(true);
        given(support.hasBids(failAuction.getId())).willReturn(false);
        given(support.findWinningBid(successAuction.getId())).willReturn(createWinningBid());

        // when
        AuctionAutoResponseDto result = useCase.execute();

        // then
        assertThat(result.getProcessedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(1);
    }
}