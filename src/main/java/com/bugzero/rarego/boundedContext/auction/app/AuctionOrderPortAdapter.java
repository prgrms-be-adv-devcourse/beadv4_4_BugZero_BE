package com.bugzero.rarego.boundedContext.auction.app;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
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
    public void completeOrder(Long auctionId) {
        AuctionOrder order = auctionOrderRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
        order.complete();
    }

    private AuctionOrderDto from(AuctionOrder order) {
        return new AuctionOrderDto(
                order.getId(),
                order.getAuctionId(),
                order.getSellerId(),
                order.getBidderId(),
                order.getFinalPrice(),
                order.getStatus().name());
    }
}
