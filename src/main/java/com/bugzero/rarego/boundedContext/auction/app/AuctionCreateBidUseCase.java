package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionBidCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
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
    private final ProductRepository productRepository;
    private final PaymentApiClient paymentApiClient; // dev에서 추가된 의존성
    private final ApplicationEventPublisher eventPublisher; // 질문자님이 추가한 SSE 발행자

    @Transactional
    public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, Long memberId, int bidAmount) {

        // 1. 회원 조회
        AuctionMember bidder = auctionMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

        // 2. 경매 조회 (비관적 락 적용)
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

        // 3. 상품 조회 (판매자 본인 입찰 확인용)
        Product product = productRepository.findById(auction.getProductId())
                .orElseThrow(() -> new CustomException(ErrorType.INTERNAL_SERVER_ERROR));

        // 4. 유효성 검증 (경매 상태, 연속 입찰, 시간, 최소 금액 등)
        validateBid(auction, product, memberId, bidAmount);

        // 5. 보증금 Hold 처리 (dev 반영 사항)
        // 정책: 경매 시작 금액의 10%를 보증금으로 책정
        int depositAmount = (int) (auction.getStartPrice() * 0.1);
        // 결제 모듈 호출 (실패 시 예외 발생으로 전체 트랜잭션 롤백됨)
        DepositHoldResponseDto depositResponse = paymentApiClient.holdDeposit(depositAmount, memberId, auctionId);

        // 6. 가격 업데이트
        if (auction.getCurrentPrice() == null || !auction.getCurrentPrice().equals(bidAmount)) {
            auction.updateCurrentPrice(bidAmount);
        }

        // 7. 입찰 기록 저장
        Bid bid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(memberId)
                .bidAmount(bidAmount)
                .build();

        bidRepository.save(bid);

        // 8. 실시간 SSE 입찰 이벤트 발행 (질문자님 반영 사항 - 매우 중요!)
        eventPublisher.publishEvent(
                AuctionBidCreatedEvent.of(auctionId, memberId, bidAmount)
        );

        // 9. 응답 객체 생성
        BidResponseDto responseDto = BidResponseDto.from(
                bid,
                bidder.getPublicId(),
                Long.valueOf(auction.getCurrentPrice())
        );

        return SuccessResponseDto.from(SuccessType.CREATED, responseDto);
    }

    public void validateBid(Auction auction, Product product, Long memberId, int bidAmount) {
        // 경매 상태 체크
        if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
            throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS, "경매가 진행 중인 상태가 아닙니다.");
        }

        // 연속 입찰 방지 (현재 최고 입찰자 체크)
        Optional<Bid> lastBid = bidRepository.findTopByAuctionIdOrderByBidTimeDesc(auction.getId());
        if (lastBid.isPresent() && lastBid.get().getBidderId().equals(memberId)) {
            throw new CustomException(ErrorType.AUCTION_ALREADY_HIGHEST_BIDDER);
        }

        // 판매자 본인 입찰 방지
        if (product.getSellerId().equals(memberId)) {
            throw new CustomException(ErrorType.AUCTION_SELLER_CANNOT_BID, "본인 경매에는 입찰할 수 없습니다.");
        }

        // 경매 입찰 가능 시간 검증
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) {
            throw new CustomException(ErrorType.AUCTION_TIME_INVALID, "입찰 가능한 시간이 아닙니다.");
        }

        // 최소 입찰 금액 계산 (첫 입찰은 시작가, 이후는 현재가 + 최소 단위)
        int nextMinBid = (auction.getCurrentPrice() == null)
                ? auction.getStartPrice()
                : auction.getCurrentPrice() + auction.getTickSize();

        if (bidAmount < nextMinBid) {
            throw new CustomException(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW,
                    String.format("최소 입찰가(%d)보다 낮습니다.", nextMinBid));
        }
    }
}