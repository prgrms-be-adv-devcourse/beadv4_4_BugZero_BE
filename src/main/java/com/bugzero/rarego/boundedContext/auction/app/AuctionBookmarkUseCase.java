package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionBookmarkRepository;
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

    @Transactional
    public WishlistAddResponseDto addBookmark(Long memberId, Long auctionId) {
        Auction auction = auctionSupport.findAuctionById(auctionId);

        // 2. 중복 확인
        if (auctionBookmarkRepository.existsByAuctionIdAndMemberId(auctionId, memberId))
            throw new CustomException(ErrorType.BOOKMARK_ALREADY_EXISTS);

        // 3. 북마크 저장
        AuctionBookmark bookmark = AuctionBookmark.builder()
                .memberId(memberId)
                .auctionId(auctionId)
                .productId(auction.getProductId())
                .build();

        auctionBookmarkRepository.save(bookmark);

        return WishlistAddResponseDto.of(true, auctionId);
    }
}