package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistRemoveResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionBookmarkRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

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
		String publicId = "member-public-id";
		Long memberId = 1L;
		Long auctionId = 1L;

		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000)
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(1))
			.durationDays(1)
			.sellerId(1L)
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);

		AuctionMember member = AuctionMember.builder()
			.id(memberId)
			.publicId(publicId)
			.build();

		given(auctionSupport.getPublicMember(publicId))
			.willReturn(member);
		given(auctionSupport.findAuctionById(auctionId))
			.willReturn(auction);

		given(auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, memberId))
			.willReturn(false);

		// when
		WishlistAddResponseDto result = auctionBookmarkUseCase.addBookmark(publicId, auctionId);

		// then
		assertThat(result.bookmarked()).isTrue();
		assertThat(result.auctionId()).isEqualTo(auctionId);

		verify(auctionBookmarkRepository).save(argThat(bookmark -> bookmark.getMemberId().equals(memberId) &&
			bookmark.getAuctionId().equals(auctionId) &&
			bookmark.getProductId().equals(50L)));
	}

	@Test
	@DisplayName("관심 경매 등록 - 이미 북마크된 경우 예외 발생")
	void addBookmark_AlreadyBookmarked() {
		// given
		String publicId = "member-public-id";
		Long memberId = 100L;
		Long auctionId = 1L;

		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000)
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(1))
			.durationDays(1)
			.sellerId(1L)
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);

		AuctionMember member = AuctionMember.builder()
			.id(memberId)
			.publicId(publicId)
			.build();

		given(auctionSupport.findAuctionById(auctionId))
			.willReturn(auction);
		given(auctionSupport.getPublicMember(publicId))
			.willReturn(member);

		given(auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, memberId))
			.willReturn(true);

		// when & then
		assertThatThrownBy(() -> auctionBookmarkUseCase.addBookmark(publicId, auctionId))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> assertThat(((CustomException)ex).getErrorType())
				.isEqualTo(ErrorType.BOOKMARK_ALREADY_EXISTS));

		verify(auctionBookmarkRepository, never()).save(any(AuctionBookmark.class));
	}

	@Test
	@DisplayName("관심 경매 해제 - 성공")
	void removeBookmark_Success() {
		// given
		String publicId = "test-public-id";
		Long memberId = 100L;
		Long auctionId = 500L;

		AuctionMember member = AuctionMember.builder().publicId(publicId).build();
		ReflectionTestUtils.setField(member, "id", memberId);

		AuctionBookmark bookmark = AuctionBookmark.builder()
			.memberId(memberId)
			.auctionId(500L)
			.productId(50L)
			.build();
		ReflectionTestUtils.setField(bookmark, "id", 1L);

		given(auctionSupport.getPublicMember(publicId))
			.willReturn(member);

		given(auctionBookmarkRepository.findByAuctionIdAndMemberId(auctionId, memberId))
			.willReturn(Optional.of(bookmark));

		// when
		WishlistRemoveResponseDto result = auctionBookmarkUseCase.removeBookmark(publicId, auctionId);

		// then
		assertThat(result.removed()).isTrue();
		assertThat(result.auctionId()).isEqualTo(auctionId);

		verify(auctionBookmarkRepository).delete(bookmark);
	}

	@Test
	@DisplayName("관심 경매 해제 - 북마크를 찾을 수 없는 경우 예외 발생")
	void removeBookmark_BookmarkNotFound() {
		// given
		String publicId = "test-public-id";
		Long memberId = 100L;
		Long auctionId = 500L;

		AuctionMember member = AuctionMember.builder().publicId(publicId).build();
		ReflectionTestUtils.setField(member, "id", memberId);

		given(auctionSupport.getPublicMember(publicId))
			.willReturn(member);

		// 존재하지 않는 북마크 ID 조회 시나리오
		given(auctionBookmarkRepository.findByAuctionIdAndMemberId(auctionId, memberId))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> auctionBookmarkUseCase.removeBookmark(publicId, auctionId))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> assertThat(((CustomException)ex).getErrorType())
				.isEqualTo(ErrorType.BOOKMARK_NOT_FOUND));

		verify(auctionBookmarkRepository, never()).delete(any(AuctionBookmark.class));
	}

}