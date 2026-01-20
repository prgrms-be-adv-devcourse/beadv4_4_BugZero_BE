package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionBidCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.payment.out.PaymentApiClient;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuctionCreateBidUseCase {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionMemberRepository auctionMemberRepository;
    private final PaymentApiClient paymentApiClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, String memberPublicId, int bidAmount) {
        // 1. 회원 조회 (Public ID 기반)
        AuctionMember bidder = auctionMemberRepository.findByPublicId(memberPublicId)
                .orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

        // 2. 경매 조회 (비관적 락 적용으로 동시성 해결)
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

        // 3. 유효성 검증 (상태, 본인 입찰, 연속 입찰, 시간, 최소 금액)
        validateBid(auction, bidder, bidAmount);

        // 4. 보증금 Hold 처리 (실제 결제 모듈 호출)
        // 정책: 경매 시작 금액의 10%를 보증금으로 책정
        int depositAmount = (int) (auction.getStartPrice() * 0.1);
        paymentApiClient.holdDeposit(depositAmount, bidder.getId(), auctionId);

        // 5. 경매 가격 업데이트
        auction.updateCurrentPrice(bidAmount);

        // 6. 입찰 기록 저장
        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidder.getId())
                .bidAmount(bidAmount)
                .build();
        bidRepository.save(bid);

        // 7. 실시간 SSE 및 푸시 알림을 위한 이벤트 발행
        eventPublisher.publishEvent(
                AuctionBidCreatedEvent.of(auctionId, bidder.getId(), bidAmount)
        );

        // 8. 응답 객체 생성 및 SuccessResponseDto로 감싸기
        BidResponseDto responseDto = BidResponseDto.from(
                bid,
                bidder.getPublicId(),
                Long.valueOf(auction.getCurrentPrice())
        );

        return SuccessResponseDto.from(SuccessType.CREATED, responseDto);
    }

    private void validateBid(Auction auction, AuctionMember bidder, int bidAmount) {
        // 경매 상태 체크
        if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
            throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS);
        }

        // 판매자 본인 입찰 방지 (Auction 엔티티의 sellerId 활용)
        if (auction.getSellerId().equals(bidder.getId())) {
            throw new CustomException(ErrorType.AUCTION_SELLER_CANNOT_BID);
        }

        // 연속 입찰 방지 (현재 최고 입찰자가 본인이면 금지)
        bidRepository.findTopByAuctionIdOrderByBidTimeDesc(auction.getId())
            .ifPresent(lastBid -> {
                if (lastBid.getBidderId().equals(bidder.getId())) {
                    throw new CustomException(ErrorType.AUCTION_ALREADY_HIGHEST_BIDDER);
                }
            });

        // 경매 가능 시간 검증
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) {
            throw new CustomException(ErrorType.AUCTION_TIME_INVALID);
        }

        // 최소 입찰 금액 검증 (현재가 + 호가 단위)
        int minRequired = (auction.getCurrentPrice() == null) 
                          ? auction.getStartPrice() 
                          : auction.getCurrentPrice() + auction.getTickSize();

        if (bidAmount < minRequired) {
            throw new CustomException(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW);
        }
    }
}