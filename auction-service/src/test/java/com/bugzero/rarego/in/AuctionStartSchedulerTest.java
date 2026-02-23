package com.bugzero.rarego.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.global.outbox.app.OutboxUseCase;
import com.bugzero.rarego.out.AuctionBookmarkRepository;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.out.es.ProductSearchClient;
import com.bugzero.rarego.shared.auction.event.AuctionStartedEvent;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;

@ExtendWith(MockitoExtension.class)
class AuctionStartSchedulerTest {

    @InjectMocks
    private AuctionStartScheduler auctionStartScheduler;

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private AuctionBookmarkRepository auctionBookmarkRepository;
    @Mock
    private OutboxUseCase outboxUseCase;
    @Mock
    private ProductSearchClient productSearchClient;

    @Captor
    private ArgumentCaptor<Object> outboxCaptor;

    void injectSelf() {
        ReflectionTestUtils.setField(auctionStartScheduler, "self", auctionStartScheduler);
    }

    @Test
    @DisplayName("autoStart starts scheduled auctions and writes outbox")
    void autoStartAuctions_Success() {
        injectSelf();

        Long auctionId = 1L;
        Long productId = 100L;
        String productName = "test-product";
        List<Long> bookmarkedMemberIds = List.of(10L, 20L, 30L);
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);

        Auction auction = Auction.builder()
            .productId(productId)
            .sellerId(1L)
            .startPrice(10000)
            .durationDays(3)
            .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.SCHEDULED);
        ReflectionTestUtils.setField(auction, "startTime", startTime);

        Page<Auction> firstPage = new PageImpl<>(List.of(auction));
        Page<Auction> emptyPage = new PageImpl<>(List.of());

        given(auctionRepository.findAllByStatusAndStartTimeBefore(eq(AuctionStatus.SCHEDULED), any(), any(Pageable.class)))
            .willReturn(firstPage, emptyPage);
        given(auctionRepository.findByIdWithLock(auctionId))
            .willReturn(Optional.of(auction));
        given(auctionBookmarkRepository.findMemberIdsByAuctionId(auctionId))
            .willReturn(bookmarkedMemberIds);
        given(productSearchClient.getProduct(productId))
            .willReturn(Optional.of(ProductAuctionResponseDto.builder().name(productName).build()));

        auctionStartScheduler.autoStartAuctions();

        verify(outboxUseCase).saveOutbox(outboxCaptor.capture());
        AuctionStartedEvent event = (AuctionStartedEvent)outboxCaptor.getValue();
        assertThat(event.auctionId()).isEqualTo(auctionId);
        assertThat(event.productId()).isEqualTo(productId);
        assertThat(event.productName()).isEqualTo(productName);
        assertThat(event.bookmarkedMemberIds()).isEqualTo(bookmarkedMemberIds);
        assertThat(event.startedAt()).isEqualTo(startTime);
    }

    @Test
    @DisplayName("autoStart does nothing when there is no pending auction")
    void autoStartAuctions_NoPendingAuctions() {
        injectSelf();

        Page<Auction> emptyPage = new PageImpl<>(List.of());
        given(auctionRepository.findAllByStatusAndStartTimeBefore(eq(AuctionStatus.SCHEDULED), any(), any(Pageable.class)))
            .willReturn(emptyPage);

        auctionStartScheduler.autoStartAuctions();

        verify(outboxUseCase, never()).saveOutbox(any());
        verify(auctionBookmarkRepository, never()).findMemberIdsByAuctionId(any());
    }

    @Test
    @DisplayName("when product lookup fails, fallback name is used")
    void autoStartAuctions_ProductSearchFails() {
        injectSelf();

        Long auctionId = 2L;
        Long productId = 200L;
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);

        Auction auction = Auction.builder()
            .productId(productId)
            .sellerId(1L)
            .startPrice(10000)
            .durationDays(3)
            .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.SCHEDULED);
        ReflectionTestUtils.setField(auction, "startTime", startTime);

        Page<Auction> firstPage = new PageImpl<>(List.of(auction));
        Page<Auction> emptyPage = new PageImpl<>(List.of());

        given(auctionRepository.findAllByStatusAndStartTimeBefore(eq(AuctionStatus.SCHEDULED), any(), any(Pageable.class)))
            .willReturn(firstPage, emptyPage);
        given(auctionRepository.findByIdWithLock(auctionId))
            .willReturn(Optional.of(auction));
        given(auctionBookmarkRepository.findMemberIdsByAuctionId(auctionId))
            .willReturn(List.of());
        given(productSearchClient.getProduct(productId))
            .willThrow(new RuntimeException("ES connection failed"));

        assertThatNoException().isThrownBy(() -> auctionStartScheduler.autoStartAuctions());

        verify(outboxUseCase).saveOutbox(outboxCaptor.capture());
        AuctionStartedEvent event = (AuctionStartedEvent)outboxCaptor.getValue();
        assertThat(event.productName()).isEqualTo("Unknown Product");
    }

    @Test
    @DisplayName("one auction failure does not block the rest")
    void autoStartAuctions_PartialFailure() {
        injectSelf();

        Long auctionId1 = 1L;
        Long auctionId2 = 2L;
        Long productId = 100L;

        Auction auction1 = Auction.builder()
            .productId(productId).sellerId(1L).startPrice(10000).durationDays(3).build();
        ReflectionTestUtils.setField(auction1, "id", auctionId1);
        ReflectionTestUtils.setField(auction1, "status", AuctionStatus.SCHEDULED);
        ReflectionTestUtils.setField(auction1, "startTime", LocalDateTime.now().minusMinutes(1));

        Auction auction2 = Auction.builder()
            .productId(productId).sellerId(1L).startPrice(10000).durationDays(3).build();
        ReflectionTestUtils.setField(auction2, "id", auctionId2);
        ReflectionTestUtils.setField(auction2, "status", AuctionStatus.SCHEDULED);
        ReflectionTestUtils.setField(auction2, "startTime", LocalDateTime.now().minusMinutes(1));

        Page<Auction> firstPage = new PageImpl<>(List.of(auction1, auction2));
        Page<Auction> emptyPage = new PageImpl<>(List.of());

        given(auctionRepository.findAllByStatusAndStartTimeBefore(eq(AuctionStatus.SCHEDULED), any(), any(Pageable.class)))
            .willReturn(firstPage, emptyPage);
        given(auctionRepository.findByIdWithLock(auctionId1))
            .willThrow(new RuntimeException("DB error"));
        given(auctionRepository.findByIdWithLock(auctionId2))
            .willReturn(Optional.of(auction2));
        given(auctionBookmarkRepository.findMemberIdsByAuctionId(auctionId2))
            .willReturn(List.of());
        given(productSearchClient.getProduct(productId))
            .willReturn(Optional.of(ProductAuctionResponseDto.builder().name("product").build()));

        assertThatNoException().isThrownBy(() -> auctionStartScheduler.autoStartAuctions());

        verify(outboxUseCase, times(1)).saveOutbox(any());
    }
}
