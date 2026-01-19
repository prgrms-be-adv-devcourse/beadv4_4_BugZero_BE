package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionBookmarkRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.shared.member.out.MemberApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionBookmarkUseCaseTest {

    @Mock
    private AuctionBookmarkRepository auctionBookmarkRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private MemberApiClient memberApiClient;

    @InjectMocks
    private AuctionBookmarkUseCase auctionBookmarkUseCase;

    @Test
    @DisplayName("관심 경매 등록 - 성공")
    void addBookmark_Success() {
        // given
        String memberUUID = "test-uuid";
        Long memberId = 100L;
        Long auctionId = 1L;

        Auction auction = Auction.builder()
                .productId(50L)
                .startPrice(1000)
                .tickSize(100)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .durationDays(1)
                .sellerId(1L)
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);

        // 모의 객체 설정
        given(memberApiClient.findMemberIdByPublicId(memberUUID))
                .willReturn(memberId);

        given(auctionRepository.findById(auctionId))
                .willReturn(Optional.of(auction));

        given(auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, memberId))
                .willReturn(false);

        // when
        WishlistAddResponseDto result = auctionBookmarkUseCase.addBookmark(memberUUID, auctionId);

        // then
        assertThat(result.bookmarked()).isTrue();
        assertThat(result.auctionId()).isEqualTo(auctionId);

        verify(auctionBookmarkRepository).save(argThat(bookmark ->
                bookmark.getMemberId().equals(memberId) &&
                        bookmark.getAuctionId().equals(auctionId) &&
                        bookmark.getProductId().equals(50L)
        ));
    }

    @Test
    @DisplayName("관심 경애 등록 - 이미 북마크된 경우")
    void addBookmark_AlreadyBookmarked() {
        // given
        String memberUUID = "test-uuid";
        Long memberId = 100L;
        Long auctionId = 1L;

        Auction auction = Auction.builder()
                .productId(50L)
                .startPrice(1000)
                .tickSize(100)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .durationDays(1)
                .sellerId(1L)
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);

        // 모의 객체 설정
        given(memberApiClient.findMemberIdByPublicId(memberUUID))
                .willReturn(memberId);

        given(auctionRepository.findById(auctionId))
                .willReturn(Optional.of(auction));

        given(auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, memberId))
                .willReturn(true);

        // when
        WishlistAddResponseDto result = auctionBookmarkUseCase.addBookmark(memberUUID, auctionId);

        // then
        assertThat(result.bookmarked()).isFalse();
        assertThat(result.auctionId()).isEqualTo(auctionId);

        verify(auctionBookmarkRepository, never()).save(any(AuctionBookmark.class));
    }
}