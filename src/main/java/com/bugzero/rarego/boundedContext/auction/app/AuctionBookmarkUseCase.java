package com.bugzero.rarego.boundedContext.auction.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistRemoveResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionBookmarkRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionBookmarkUseCase {

    private final AuctionBookmarkRepository auctionBookmarkRepository;
    private final AuctionSupport auctionSupport;

    @Transactional
    public WishlistAddResponseDto addBookmark(String publicId, Long auctionId) {
        AuctionMember member = auctionSupport.getPublicMember(publicId);

        Auction auction = auctionSupport.findAuctionById(auctionId);

        // 중복 확인
        if (auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, member.getId()))
            throw new CustomException(ErrorType.BOOKMARK_ALREADY_EXISTS);

        // 북마크 저장
        AuctionBookmark bookmark = AuctionBookmark.builder()
                .memberId(member.getId())
                .auctionId(auctionId)
                .productId(auction.getProductId())
                .build();

        auctionBookmarkRepository.save(bookmark);

        return WishlistAddResponseDto.of(true, auctionId);
    }

    @Transactional
    public WishlistRemoveResponseDto removeBookmark(String publicId, Long auctionId) {
        AuctionMember member = auctionSupport.getPublicMember(publicId);

        AuctionBookmark bookmark = auctionBookmarkRepository.findByAuctionIdAndMemberId(auctionId, member.getId())
                .orElseThrow(() -> new CustomException(ErrorType.BOOKMARK_NOT_FOUND));

        auctionBookmarkRepository.delete(bookmark);

        return WishlistRemoveResponseDto.of(true, auctionId);
    }
}