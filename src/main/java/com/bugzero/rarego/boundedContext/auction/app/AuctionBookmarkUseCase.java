package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionBookmarkRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.out.MemberApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionBookmarkUseCase {

    private final AuctionBookmarkRepository auctionBookmarkRepository;
    private final AuctionRepository auctionRepository;
    private final MemberApiClient memberApiClient;

    @Transactional
    public WishlistAddResponseDto addBookmark(String memberUUID, Long auctionId) {
        // 멤버 조회
        Long memberId = memberApiClient.findMemberIdByPublicId(memberUUID);

        // 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

        // 중복 북마크 확인 및 처리
        boolean isAlreadyBookmarked = auctionBookmarkRepository
                .existsByAuctionIdAndMemberId(auctionId, memberId);

        if (isAlreadyBookmarked) {
            return WishlistAddResponseDto.of(false, auctionId);
        }

        // 북마크 생성
        AuctionBookmark bookmark = AuctionBookmark.builder()
                .memberId(memberId)
                .auctionId(auctionId)
                .productId(auction.getProductId())
                .build();

        auctionBookmarkRepository.save(bookmark);

        return WishlistAddResponseDto.of(true, auctionId);
    }
}