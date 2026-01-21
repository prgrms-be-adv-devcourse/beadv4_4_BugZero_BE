package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistListResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistRemoveResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionBookmarkRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
    private AuctionSupport auctionSupport;

    @InjectMocks
    private AuctionBookmarkUseCase auctionBookmarkUseCase;

    @Mock
    private AuctionMemberRepository auctionMemberRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Test
    @DisplayName("관심 경매 등록 - 성공")
    void addBookmark_Success() {
        // given
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

        given(auctionSupport.findAuctionById(auctionId))
                .willReturn(auction);

        given(auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, memberId))
                .willReturn(false);

        // when
        WishlistAddResponseDto result = auctionBookmarkUseCase.addBookmark(memberId, auctionId);

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
    @DisplayName("관심 경매 등록 - 이미 북마크된 경우 예외 발생")
    void addBookmark_AlreadyBookmarked() {
        // given
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

        given(auctionSupport.findAuctionById(auctionId))
                .willReturn(auction);

        given(auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, memberId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> auctionBookmarkUseCase.addBookmark(memberId, auctionId))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorType())
                        .isEqualTo(ErrorType.BOOKMARK_ALREADY_EXISTS));

        verify(auctionBookmarkRepository, never()).save(any(AuctionBookmark.class));
    }

    @Test
    @DisplayName("관심 경매 해제 - 성공")
    void removeBookmark_Success() {
        // given
        String publicId = "test-public-id";
        Long memberId = 100L;
        Long bookmarkId = 1L;

        AuctionMember member = AuctionMember.builder().publicId(publicId).build();
        ReflectionTestUtils.setField(member, "id", memberId);

        AuctionBookmark bookmark = AuctionBookmark.builder()
                .memberId(memberId)
                .auctionId(500L)
                .productId(50L)
                .build();
        ReflectionTestUtils.setField(bookmark, "id", bookmarkId);

        given(auctionMemberRepository.findByPublicId(publicId))
                .willReturn(Optional.of(member));

        given(auctionBookmarkRepository.findById(bookmarkId))
                .willReturn(Optional.of(bookmark));

        // when
        WishlistRemoveResponseDto result = auctionBookmarkUseCase.removeBookmark(publicId, bookmarkId);

        // then
        assertThat(result.removed()).isTrue();
        assertThat(result.bookmarkId()).isEqualTo(bookmarkId);

        verify(auctionBookmarkRepository).delete(bookmark);
    }

    @Test
    @DisplayName("관심 경매 해제 - 북마크를 찾을 수 없는 경우 예외 발생")
    void removeBookmark_BookmarkNotFound() {
        // given
        String publicId = "test-public-id";
        Long memberId = 100L;
        Long bookmarkId = 1L;

        AuctionMember member = AuctionMember.builder().publicId(publicId).build();
        ReflectionTestUtils.setField(member, "id", memberId);

        given(auctionMemberRepository.findByPublicId(publicId))
                .willReturn(Optional.of(member));

        // 존재하지 않는 북마크 ID 조회 시나리오
        given(auctionBookmarkRepository.findById(bookmarkId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionBookmarkUseCase.removeBookmark(publicId, bookmarkId))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorType())
                        .isEqualTo(ErrorType.BOOKMARK_NOT_FOUND));

        verify(auctionBookmarkRepository, never()).delete(any(AuctionBookmark.class));
    }

    @Test
    @DisplayName("관심 경매 해제 - 소유자가 아닌 경우 예외 발생")
    void removeBookmark_UnauthorizedAccess() {
        // given
        String publicId = "test-public-id";
        Long requesterId = 100L; // 요청자
        Long ownerId = 200L;     // 실제 북마크 주인 (다름)
        Long bookmarkId = 1L;

        AuctionMember member = AuctionMember.builder().publicId(publicId).build();
        ReflectionTestUtils.setField(member, "id", requesterId);

        AuctionBookmark bookmark = AuctionBookmark.builder()
                .memberId(ownerId) // 주인을 다르게 설정
                .build();
        ReflectionTestUtils.setField(bookmark, "id", bookmarkId);

        given(auctionMemberRepository.findByPublicId(publicId))
                .willReturn(Optional.of(member));

        given(auctionBookmarkRepository.findById(bookmarkId))
                .willReturn(Optional.of(bookmark));

        // when & then
        assertThatThrownBy(() -> auctionBookmarkUseCase.removeBookmark(publicId, bookmarkId))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorType())
                        .isEqualTo(ErrorType.BOOKMARK_UNAUTHORIZED_ACCESS));

        verify(auctionBookmarkRepository, never()).delete(any(AuctionBookmark.class));
    }

    @Test
    @DisplayName("내 관심 경매 목록 조회 - 성공")
    void getMyBookmarks_Success() {
        // given
        String publicId = "test-public-id";
        Long memberId = 100L;
        Long auctionId = 1L;
        Long bookmarkId = 10L;
        Pageable pageable = PageRequest.of(0, 10);

        AuctionMember member = AuctionMember.builder().publicId(publicId).build();
        ReflectionTestUtils.setField(member, "id", memberId);

        AuctionBookmark bookmark = AuctionBookmark.builder()
                .memberId(memberId)
                .auctionId(auctionId)
                .productId(50L)
                .build();
        ReflectionTestUtils.setField(bookmark, "id", bookmarkId);

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
        auction.startAuction();
        auction.updateCurrentPrice(15000);

        Page<AuctionBookmark> bookmarkPage = new PageImpl<>(List.of(bookmark), pageable, 1);

        given(auctionMemberRepository.findByPublicId(publicId))
                .willReturn(Optional.of(member));

        given(auctionBookmarkRepository.findAllByMemberId(memberId, pageable))
                .willReturn(bookmarkPage);

        given(auctionRepository.findAllById(Set.of(auctionId)))
                .willReturn(List.of(auction));

        // when
        PagedResponseDto<WishlistListResponseDto> result = auctionBookmarkUseCase.getMyBookmarks(publicId, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).bookmarkId()).isEqualTo(bookmarkId);
        assertThat(result.data().get(0).auctionId()).isEqualTo(auctionId);
        assertThat(result.data().get(0).productId()).isEqualTo(50L);
        assertThat(result.data().get(0).auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
        assertThat(result.data().get(0).currentPrice()).isEqualTo(15000);
    }

    @Test
    @DisplayName("내 관심 경매 목록 조회 - 회원을 찾을 수 없는 경우 예외 발생")
    void getMyBookmarks_MemberNotFound() {
        // given
        String publicId = "non-existent-public-id";
        Pageable pageable = PageRequest.of(0, 10);

        given(auctionMemberRepository.findByPublicId(publicId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionBookmarkUseCase.getMyBookmarks(publicId, pageable))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorType())
                        .isEqualTo(ErrorType.MEMBER_NOT_FOUND));
    }
}