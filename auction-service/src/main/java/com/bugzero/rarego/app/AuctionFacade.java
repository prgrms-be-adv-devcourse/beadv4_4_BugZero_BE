package com.bugzero.rarego.app;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.domain.AuctionMember;
import com.bugzero.rarego.domain.AuctionOrderStatus;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.in.dto.AuctionAddBookmarkResponseDto;
import com.bugzero.rarego.in.dto.AuctionBookmarkListResponseDto;
import com.bugzero.rarego.in.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.in.dto.AuctionFilterType;
import com.bugzero.rarego.in.dto.AuctionListResponseDto;
import com.bugzero.rarego.in.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.in.dto.AuctionRelistRequestDto;
import com.bugzero.rarego.in.dto.AuctionRelistResponseDto;
import com.bugzero.rarego.in.dto.AuctionRemoveBookmarkResponseDto;
import com.bugzero.rarego.in.dto.AuctionSearchCondition;
import com.bugzero.rarego.in.dto.AuctionWithdrawResponseDto;
import com.bugzero.rarego.in.dto.BidLogResponseDto;
import com.bugzero.rarego.in.dto.BidResponseDto;
import com.bugzero.rarego.in.dto.MyAuctionOrderListResponseDto;
import com.bugzero.rarego.in.dto.MyBidResponseDto;
import com.bugzero.rarego.in.dto.MySaleResponseDto;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.payment.out.PaymentApiClient;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionFacade {

    private final AuctionCreateBidUseCase auctionCreateBidUseCase;
    private final AuctionReadUseCase auctionReadUseCase;
    private final AuctionSyncMemberUseCase auctionSyncMemberUseCase;
    private final AuctionBookmarkUseCase auctionBookmarkUseCase;
    private final AuctionRelistUseCase auctionRelistUseCase;
    private final AuctionWithdrawUseCase auctionWithdrawUseCase;
    private final AuctionCreateAuctionUseCase auctionCreateAuctionUseCase;
    private final AuctionUpdateAuctionUseCase auctionUpdateAuctionUseCase;
    private final AuctionDeleteAuctionUseCase auctionDeleteAuctionUseCase;
    private final AuctionDetermineStartAuctionUseCase auctionDetermineStartAuctionUseCase;
    private final AuctionSubscribeStreamUseCase auctionSubscribeStreamUseCase;
    private final PaymentApiClient paymentApiClient;
    private final AuctionSupport support;

    // 쓰기 작업 (입찰 생성)
    // TODO: 경매 모듈쪽으로 보증금 홀드하는 api 호출 부분을 카프카 이벤트로 처리 필요
    public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, String memberPublicId, int bidAmount) {

        // 보증금 계산 및 선결제(Hold) 요청
        // 락 진입 전에 수행하므로 락 점유 시간을 획기적으로 줄임
        Auction auction = support.findAuctionById(auctionId);
        int depositAmount = (int) (auction.getStartPrice() * 0.1);

        paymentApiClient.holdDeposit(depositAmount, memberPublicId, auctionId);

        try {
            BidResponseDto result = auctionCreateBidUseCase.createBid(auctionId, memberPublicId, bidAmount);

            return SuccessResponseDto.from(SuccessType.CREATED, result);

        } catch (Exception e) {
            log.error("입찰 실패로 인한 보증금 취소 요청: auctionId={}, error={}", auctionId, e.getMessage());
            try {
                paymentApiClient.releaseDeposit(auctionId, memberPublicId);
            } catch (Exception payEx) {
                log.error("CRITICAL: 보증금 취소 실패! 수동 확인 요망.", payEx);
            }
            throw e;
        }
    }

    // 재경매 생성
    public SuccessResponseDto<AuctionRelistResponseDto> relistAuction(Long auctionId, String memberPublicId,
                                                                      AuctionRelistRequestDto request) {
        AuctionRelistResponseDto result = auctionRelistUseCase.relistAuction(auctionId, memberPublicId, request);
        return SuccessResponseDto.from(SuccessType.OK, result);
    }

    // 읽기 작업

    // 입찰 기록 조회
    public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
        return auctionReadUseCase.getBidLogs(auctionId, pageable);
    }

    // 내 입찰 내역
    public PagedResponseDto<MyBidResponseDto> getMyBids(String memberPublicId, AuctionStatus status,
                                                        Pageable pageable) {
        return auctionReadUseCase.getMyBids(memberPublicId, status, pageable);
    }

    // 내 판매 내역
    public PagedResponseDto<MySaleResponseDto> getMySales(String memberPublicId, AuctionFilterType auctionFilterType,
                                                          Pageable pageable) {
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
    public AuctionAddBookmarkResponseDto addBookmark(String publicId, Long auctionId) {
        return auctionBookmarkUseCase.addBookmark(publicId, auctionId);
    }

    // 경매 상태/현재가 요약 조회
    public PagedResponseDto<AuctionListResponseDto> getAuctions(AuctionSearchCondition condition, Pageable pageable) {
        return auctionReadUseCase.getAuctions(condition, pageable);
    }

    // 나의 낙찰 목록 조회
    public PagedResponseDto<MyAuctionOrderListResponseDto> getMyAuctionOrders(String memberPublicId,
                                                                              AuctionOrderStatus status, Pageable pageable) {
        return auctionReadUseCase.getMyAuctionOrders(memberPublicId, status, pageable);
    }

    public AuctionMember syncMember(MemberDto member) {
        return auctionSyncMemberUseCase.syncMember(member);
    }

    // 관심 경매 해제
    public AuctionRemoveBookmarkResponseDto removeBookmark(String publicId, Long auctionId) {
        return auctionBookmarkUseCase.removeBookmark(publicId, auctionId);
    }

    // 내 관심 경매 목록 조회
    public PagedResponseDto<AuctionBookmarkListResponseDto> getMyBookmarks(String publicId, Pageable pageable) {
        return auctionReadUseCase.getMyBookmarks(publicId, pageable);
    }

    // 판매 포기
    public AuctionWithdrawResponseDto withdraw(Long auctionId, String memberPublicId) {
        return auctionWithdrawUseCase.execute(auctionId, memberPublicId);
    }

    public boolean hasActiveBids(String publicId) {
        return auctionWithdrawUseCase.hasActiveBids(publicId);
    }

    public boolean hasActiveSales(String publicId) {
        return auctionWithdrawUseCase.hasActiveSales(publicId);
    }

    //경매 주문이 진행중인지 확인
    public boolean hasProcessingOrders(String publicId) {

        return auctionWithdrawUseCase.hasProcessingOrders(publicId);
    }

    // 경매 정보 생성
    public Long createAuction(Long productId, String publicId, ProductAuctionRequestDto productAuctionRequestDto) {
        return auctionCreateAuctionUseCase.createAuction(productId, publicId, productAuctionRequestDto);
    }

    // 경매 정보 수정
    public Long updateAuction(String publicId, ProductAuctionUpdateDto dto) {
        return auctionUpdateAuctionUseCase.updateAuction(publicId, dto);
    }

    // 경매 정보 삭제
    public void deleteAuction(String publicId, Long productId) {
        auctionDeleteAuctionUseCase.deleteAuction(publicId, productId);
    }

    public Long determineStartAuction(Long productId) {
        return auctionDetermineStartAuctionUseCase.determineStartAuction(productId);
    }

    public SseEmitter subscribeAuctionStream(Long auctionId) {
        return auctionSubscribeStreamUseCase.execute(auctionId);
    }

    public int getTotalSubscribers() {
        return auctionSubscribeStreamUseCase.getTotalSubscribers();
    }

    public int getAuctionSubscribers(Long auctionId) {
        return auctionSubscribeStreamUseCase.getAuctionSubscribers(auctionId);
    }
}
