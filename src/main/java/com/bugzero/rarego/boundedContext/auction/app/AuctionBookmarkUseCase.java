package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistListResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistRemoveResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionBookmarkRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionBookmarkUseCase {

    private final AuctionBookmarkRepository auctionBookmarkRepository;
    private final AuctionSupport auctionSupport;
    private final AuctionMemberRepository auctionMemberRepository;
    private final AuctionRepository auctionRepository;

    @Transactional
    public WishlistAddResponseDto addBookmark(Long memberId, Long auctionId) {
        Auction auction = auctionSupport.findAuctionById(auctionId);

        // 중복 확인
        if (auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, memberId))
            throw new CustomException(ErrorType.BOOKMARK_ALREADY_EXISTS);

        // 북마크 저장
        AuctionBookmark bookmark = AuctionBookmark.builder()
                .memberId(memberId)
                .auctionId(auctionId)
                .productId(auction.getProductId())
                .build();

        auctionBookmarkRepository.save(bookmark);

        return WishlistAddResponseDto.of(true, auctionId);
    }

    @Transactional
    public WishlistRemoveResponseDto removeBookmark(String publicId, Long bookmarkId) {
        AuctionMember member = auctionMemberRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

        AuctionBookmark bookmark = auctionBookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new CustomException(ErrorType.BOOKMARK_NOT_FOUND));

        // 해당 북마크의 주인이 현재 요청한 사용자가 맞는지 체크
        if (!bookmark.getMemberId().equals(member.getId())) {
            throw new CustomException(ErrorType.BOOKMARK_UNAUTHORIZED_ACCESS);
        }

        auctionBookmarkRepository.delete(bookmark);

        return WishlistRemoveResponseDto.of(true, bookmarkId);
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<WishlistListResponseDto> getMyBookmarks(String publicId, Pageable pageable) {
        AuctionMember member = auctionMemberRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

        Page<AuctionBookmark> bookmarkPage = auctionBookmarkRepository.findAllByMemberId(member.getId(), pageable);

        if (bookmarkPage.isEmpty()) {
            return PagedResponseDto.from(bookmarkPage, bookmark -> null);
        }

        Set<Long> auctionIds = bookmarkPage.getContent().stream()
                .map(AuctionBookmark::getAuctionId)
                .collect(Collectors.toSet());

        Map<Long, Auction> auctionMap = auctionRepository.findAllById(auctionIds).stream()
                .collect(Collectors.toMap(Auction::getId, Function.identity()));

        return PagedResponseDto.from(bookmarkPage, bookmark -> {
            Auction auction = auctionMap.get(bookmark.getAuctionId());
            return WishlistListResponseDto.of(
                    bookmark.getId(),
                    bookmark.getAuctionId(),
                    bookmark.getProductId(),
                    auction != null ? auction.getStatus() : null,
                    auction != null ? auction.getCurrentPrice() : null,
                    auction != null ? auction.getStartTime() : null,
                    auction != null ? auction.getEndTime() : null
            );
        });
    }
}