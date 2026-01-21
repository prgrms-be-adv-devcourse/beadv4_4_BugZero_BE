package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionWithdrawResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistListResponseDto;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistRemoveResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.*;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionFacade {

    private final AuctionCreateBidUseCase auctionCreateBidUseCase;
    private final AuctionReadUseCase auctionReadUseCase;
    private final AuctionMemberRepository auctionMemberRepository;
    private final AuctionSyncMemberUseCase auctionSyncMemberUseCase;
    private final AuctionBookmarkUseCase auctionBookmarkUseCase;
    private final AuctionWithdrawUseCase auctionWithdrawUseCase;

    // 쓰기 작업 (입찰 생성)
    @Transactional
    public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, String memberPublicId, int bidAmount) {
        BidResponseDto result = auctionCreateBidUseCase.createBid(auctionId, memberPublicId, bidAmount);
        return SuccessResponseDto.from(SuccessType.CREATED, result);
    }

    // 읽기 작업 (입찰 기록)
    public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
        return auctionReadUseCase.getBidLogs(auctionId, pageable);
    }

    // 내 입찰 내역
    public PagedResponseDto<MyBidResponseDto> getMyBids(String memberPublicId, AuctionStatus status, Pageable pageable) {
        return auctionReadUseCase.getMyBids(memberPublicId, status, pageable);
    }

    // 내 판매 내역
    public PagedResponseDto<MySaleResponseDto> getMySales(String memberPublicId, AuctionFilterType auctionFilterType, Pageable pageable) {
        return auctionReadUseCase.getMySales(memberPublicId, auctionFilterType, pageable);
    }

    // 경매 상세 조회
    public SuccessResponseDto<AuctionDetailResponseDto> getAuctionDetail(Long auctionId, String memberPublicId) {
        AuctionDetailResponseDto detail = auctionReadUseCase.getAuctionDetail(auctionId, memberPublicId);
        return SuccessResponseDto.from(SuccessType.OK, detail);
    }

    // 낙찰 기록 상세 조회
    public SuccessResponseDto<AuctionOrderResponseDto> getAuctionOrder(Long auctionId, String memberPublicId) {
        AuctionOrderResponseDto response = auctionReadUseCase.getAuctionOrder(auctionId, memberPublicId);
        return SuccessResponseDto.from(SuccessType.OK, response);
    }

    // 관심 경매 등록
    @Transactional
    public WishlistAddResponseDto addBookmark(String publicId, Long auctionId) {
        AuctionMember member = auctionMemberRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

        return auctionBookmarkUseCase.addBookmark(member.getId(), auctionId);
    }

    // 관심 경매 해제
    @Transactional
    public WishlistRemoveResponseDto removeBookmark(String publicId, Long auctionId) {
        return auctionBookmarkUseCase.removeBookmark(publicId, auctionId);
    }

    // 내 관심 경매 목록 조회
    @Transactional(readOnly = true)
    public PagedResponseDto<WishlistListResponseDto> getMyBookmarks(String publicId, Pageable pageable) {
        return auctionBookmarkUseCase.getMyBookmarks(publicId, pageable);
    }

    // 판매 포기
    @Transactional
    public AuctionWithdrawResponseDto withdraw(Long auctionId, String memberPublicId) {
        return auctionWithdrawUseCase.execute(auctionId, memberPublicId);
    }

    @Transactional
    public AuctionMember syncMember(MemberDto member) {
        return auctionSyncMemberUseCase.syncMember(member);
    }

}
