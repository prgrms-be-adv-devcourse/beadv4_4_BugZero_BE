package com.bugzero.rarego.bounded_context.auction.app;

import com.bugzero.rarego.bounded_context.auction.domain.Auction;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionBookmark;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionMember;
import com.bugzero.rarego.bounded_context.auction.in.dto.AuctionAddBookmarkResponseDto;
import com.bugzero.rarego.bounded_context.auction.in.dto.AuctionRemoveBookmarkResponseDto;
import com.bugzero.rarego.bounded_context.auction.out.AuctionBookmarkRepository;
import com.bugzero.rarego.bounded_context.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionBookmarkUseCase {

    private final AuctionBookmarkRepository auctionBookmarkRepository;
    private final AuctionSupport auctionSupport;
    private final AuctionMemberRepository auctionMemberRepository;

    @Transactional
    public AuctionAddBookmarkResponseDto addBookmark(Long memberId, Long auctionId) {
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

        return AuctionAddBookmarkResponseDto.of(true, auctionId);
    }

    @Transactional
    public AuctionRemoveBookmarkResponseDto removeBookmark(String publicId, Long bookmarkId) {
        AuctionMember member = auctionMemberRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

        AuctionBookmark bookmark = auctionBookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new CustomException(ErrorType.BOOKMARK_NOT_FOUND));

        // 해당 북마크의 주인이 현재 요청한 사용자가 맞는지 체크
        if (!bookmark.getMemberId().equals(member.getId())) {
            throw new CustomException(ErrorType.BOOKMARK_UNAUTHORIZED_ACCESS);
        }

        auctionBookmarkRepository.delete(bookmark);

        return AuctionRemoveBookmarkResponseDto.of(true, bookmarkId);
    }
}