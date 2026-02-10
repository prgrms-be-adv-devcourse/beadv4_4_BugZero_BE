package com.bugzero.rarego.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.AuctionOrder;
import com.bugzero.rarego.domain.AuctionOrderStatus;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.AuctionOrderRepository;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuctionOrderService {
	private final AuctionOrderRepository auctionOrderRepository;

	public Optional<AuctionOrderDto> findByAuctionId(Long auctionId) {
		return auctionOrderRepository.findByAuctionId(auctionId)
			.map(this::from);
	}

	public void completeOrder(Long auctionId) {
		AuctionOrder order = auctionOrderRepository.findByAuctionIdForUpdate(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
		order.complete();
	}

	public void failOrder(Long auctionId) {
		AuctionOrder order = auctionOrderRepository.findByAuctionIdForUpdate(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
		order.fail();
	}

	public AuctionOrderDto refundOrderWithLock(Long auctionId) {
		AuctionOrder order = auctionOrderRepository.findByAuctionIdForUpdate(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));

		order.refund(); // 내부에서 SUCCESS 검증 및 FAILED 변경 수행
		return from(order);
	}

	public Slice<AuctionOrderDto> findTimeoutOrders(LocalDateTime deadline, Pageable pageable) {
		return auctionOrderRepository.findByStatusAndCreatedAtBefore(AuctionOrderStatus.PROCESSING, deadline, pageable)
			.map(this::from);
	}

	public Slice<AuctionOrderDto> findExpiringSoonOrders(LocalDateTime targetEndedAt, Pageable pageable) {
		return auctionOrderRepository.findByStatusAndNoticedAtIsNullAndCreatedAtBefore(
				AuctionOrderStatus.PROCESSING,
				targetEndedAt,
				pageable
			)
			.map(this::from);

	}

	@Transactional
	public void markAsNoticed(Long orderId) {
		AuctionOrder order = auctionOrderRepository.findById(orderId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
		order.markAsNoticed();
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
