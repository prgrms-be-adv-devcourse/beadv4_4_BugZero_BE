package com.bugzero.rarego.boundedContext.auction.app;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionWithdrawResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionWithdrawUseCase {

    private final AuctionSupport auctionSupport;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionOrderRepository auctionOrderRepository;

    @Transactional
    public AuctionWithdrawResponseDto execute(Long auctionId, String memberPublicId) {
        // 회원 조회
        AuctionMember member = auctionSupport.getPublicMember(memberPublicId);

        // 경매 조회
        Auction auction = auctionSupport.findAuctionById(auctionId);

        // 판매자 본인 검증
        if (!auction.getSellerId().equals(member.getId())) {
            throw new CustomException(ErrorType.AUCTION_NOT_SELLER);
        }

        // 검수 여부 검증 (startTime이 null이면 검수 전)
        if (Objects.isNull(auction.getStartTime())) {
            throw new CustomException(ErrorType.AUCTION_WITHDRAW_NOT_INSPECTED);
        }

        // 경매 상태 검증 (ENDED인지)
        if (auction.getStatus() != AuctionStatus.ENDED) {
            throw new CustomException(ErrorType.AUCTION_WITHDRAW_NOT_ENDED);
        }

        // 주문 상태 검증 (FAILED인지 or 주문 없음)
        Optional<AuctionOrder> orderOpt = auctionSupport.findOrderByAuctionId(auctionId);
        if (orderOpt.isPresent()) {
            AuctionOrder order = orderOpt.get();

            if (order.getStatus() == AuctionOrderStatus.SUCCESS)
                throw new CustomException(ErrorType.AUCTION_WITHDRAW_ALREADY_PAID);

            if (order.getStatus() == AuctionOrderStatus.PROCESSING)
                throw new CustomException(ErrorType.AUCTION_WITHDRAW_PAYMENT_IN_PROGRESS);
        }

        // 상태 변경
        AuctionStatus beforeStatus = auction.getStatus();
        auction.withdraw();

        log.info("판매 포기 처리 완료 - auctionId: {}, sellerId: {}", auctionId, member.getId());

        return AuctionWithdrawResponseDto.of(auctionId, auction.getProductId(), beforeStatus);
    }

    public boolean hasActiveBids(String publicId) {
        return bidRepository.existsActiveBidByPublicId(publicId);
    }

    public boolean hasActiveSales(String publicId) {
        return auctionRepository.existsActiveSaleByPublicId(publicId);
    }

    public boolean hasProcessingOrders(String publicId) {
        return auctionOrderRepository.existsByBuyerPublicIdAndStatus(
                publicId, AuctionOrderStatus.PROCESSING
        );
    }
}