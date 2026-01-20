package com.bugzero.rarego.boundedContext.auction.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuctionOrderPortAdapter implements AuctionOrderPort {
    private final AuctionOrderRepository auctionOrderRepository;

    @Override
    public Optional<AuctionOrderDto> findByAuctionId(Long auctionId) {
        return auctionOrderRepository.findByAuctionId(auctionId)
                .map(this::from);
    }

    @Override
    public Optional<AuctionOrderDto> findByAuctionIdForUpdate(Long auctionId) {
        return auctionOrderRepository.findByAuctionIdForUpdate(auctionId)
                .map(this::from);
    }

    @Override
    public void completeOrder(Long auctionId) {
        AuctionOrder order = auctionOrderRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
        order.complete();
    }

    @Override
    public void failOrder(Long auctionId) {
        AuctionOrder order = auctionOrderRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
        order.fail();
    }

    @Override
    public AuctionOrderDto refundOrderWithLock(Long auctionId) {
        AuctionOrder order = auctionOrderRepository.findByAuctionIdForUpdate(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));

        order.refund(); // 내부에서 SUCCESS 검증 및 FAILED 변경 수행
        return from(order);
    }

    @Override
    public Slice<AuctionOrderDto> findTimeoutOrders(LocalDateTime deadline, Pageable pageable) {
        return auctionOrderRepository.findByStatusAndCreatedAtBefore(AuctionOrderStatus.PROCESSING, deadline, pageable)
                .map(this::from);
    }

    private AuctionOrderDto from(AuctionOrder order) {
        return new AuctionOrderDto(
                order.getId(),
                order.getAuctionId(),
                order.getSellerId(),
                order.getBidderId(),
                order.getFinalPrice(),
                order.getStatus().name(),
                order.getCreatedAt());
    }
}
