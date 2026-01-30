package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.event.AuctionFailedEvent;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionSettleOneUseCaseTest {

    @Mock
    AuctionSettlementSupport support;

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    AuctionOrderRepository auctionOrderRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    AuctionSettleOneUseCase useCase;

    private Auction createAuction(Long id, AuctionStatus status, LocalDateTime endTime) {
        Auction auction = Auction.builder()
                .productId(100L)
                .sellerId(1L)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(endTime)
                .startPrice(10_000)
                .durationDays(1) // [dev 변경사항] 필드 추가
                .build();

        ReflectionTestUtils.setField(auction, "id", id);
        ReflectionTestUtils.setField(auction, "status", status);

        return auction;
    }

    private Bid createWinningBid(Long auctionId) {
        return Bid.builder()
                .auctionId(auctionId)
                .bidderId(10L)
                .bidAmount(50_000)
                .build();
    }

    @Test
    @DisplayName("경매가 존재하지 않으면 예외 발생")
    void execute_AuctionNotFound() {
        // given
        given(auctionRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> useCase.execute(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 종료된 경매는 처리하지 않음")
    void execute_AlreadyEnded() {
        // given
        Auction auction = createAuction(1L, AuctionStatus.ENDED, LocalDateTime.now().minusHours(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when
        useCase.execute(1L);

        // then
        verify(auctionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("진행 중이 아닌 경매는 예외 발생")
    void execute_NotInProgress() {
        // given
        Auction auction = createAuction(1L, AuctionStatus.SCHEDULED, LocalDateTime.now().minusHours(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> useCase.execute(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_NOT_IN_PROGRESS);
    }

    @Test
    @DisplayName("아직 종료 시간이 안 된 경매는 예외 발생")
    void execute_NotExpiredYet() {
        // given
        Auction auction = createAuction(1L, AuctionStatus.IN_PROGRESS, LocalDateTime.now().plusHours(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> useCase.execute(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_NOT_IN_PROGRESS);
    }

    @Test
    @DisplayName("입찰이 없으면 유찰 처리")
    void execute_NoBids() {
        // given
        Auction auction = createAuction(1L, AuctionStatus.IN_PROGRESS, LocalDateTime.now().minusHours(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(support.hasBids(1L)).willReturn(false);

        // when
        useCase.execute(1L);

        // then
        verify(auctionRepository).save(auction);
        verify(eventPublisher).publishEvent(any(AuctionFailedEvent.class));
        verify(auctionOrderRepository, never()).save(any());
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
    }

    @Test
    @DisplayName("입찰이 있으면 낙찰 처리")
    void execute_WithBids() {
        // given
        Auction auction = createAuction(1L, AuctionStatus.IN_PROGRESS, LocalDateTime.now().minusHours(1));
        Bid winningBid = createWinningBid(1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(support.hasBids(1L)).willReturn(true);
        given(support.findWinningBid(1L)).willReturn(winningBid);

        // when
        useCase.execute(1L);

        // then
        verify(auctionRepository).save(auction);
        verify(auctionOrderRepository).save(any(AuctionOrder.class));
        verify(eventPublisher).publishEvent(any(AuctionEndedEvent.class));
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
    }
}