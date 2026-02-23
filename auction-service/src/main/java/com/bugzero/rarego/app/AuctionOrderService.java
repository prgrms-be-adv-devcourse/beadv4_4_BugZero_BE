package com.bugzero.rarego.app;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.domain.AuctionOrder;
import com.bugzero.rarego.domain.AuctionOrderStatus;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.AuctionOrderRepository;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.out.es.ProductSearchClient;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuctionOrderService {
	private final AuctionOrderRepository auctionOrderRepository;
	private final AuctionRepository auctionRepository;
	private final ProductSearchClient productSearchClient;
	private final TransactionTemplate transactionTemplate;

	@Transactional(readOnly = true)
	public Optional<AuctionOrderDto> findByAuctionId(Long auctionId) {
		return auctionOrderRepository.findByAuctionId(auctionId)
			.map(this::from);
	}

	@Transactional
	public void completeOrder(Long auctionId) {
		AuctionOrder order = auctionOrderRepository.findByAuctionIdForUpdate(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
		order.complete();
	}

	@Transactional
	public void failOrder(Long auctionId) {
		AuctionOrder order = auctionOrderRepository.findByAuctionIdForUpdate(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
		order.fail();
	}

	public AuctionOrderDto refundOrderWithLock(Long auctionId) {
		// 트랜잭션 내에서 데이터 수정 (LOCK 수행)
		AuctionOrder order = transactionTemplate.execute(status -> {
			AuctionOrder auctionOrder = auctionOrderRepository.findByAuctionIdForUpdate(auctionId)
				.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));

			auctionOrder.refund(); // 상태 변경 및 더티 체킹 발생
			return auctionOrder;
			// 메서드 종료 시 커밋되며 락 해제
		});
		return from(order);
	}

	@Transactional(readOnly = true)
	public Slice<AuctionOrderDto> findTimeoutOrders(LocalDateTime deadline, Pageable pageable) {
		Slice<AuctionOrder> orders = auctionOrderRepository.findByStatusAndCreatedAtBefore(
			AuctionOrderStatus.PROCESSING,
			deadline,
			pageable
		);
		return toDtoSlice(orders);
	}

	@Transactional(readOnly = true)
	public Slice<AuctionOrderDto> findExpiringSoonOrders(LocalDateTime targetEndedAt, Pageable pageable) {
		Slice<AuctionOrder> orders = auctionOrderRepository.findByStatusAndNoticedAtIsNullAndCreatedAtBefore(
				AuctionOrderStatus.PROCESSING,
				targetEndedAt,
				pageable
			);
		return toDtoSlice(orders);
	}

	@Transactional
	public void markAsNoticed(Long orderId) {
		AuctionOrder order = auctionOrderRepository.findById(orderId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
		order.markAsNoticed();
	}

	private AuctionOrderDto from(AuctionOrder order) {
		String productName = auctionRepository.findById(order.getAuctionId())
			.map(Auction::getProductId)
			.flatMap(productSearchClient::getProduct)
			.map(product -> product.name())
			.orElse("Unknown Product");

		return new AuctionOrderDto(
			order.getId(),
			order.getAuctionId(),
			order.getSellerId(),
			order.getBidderId(),
			order.getFinalPrice(),
			order.getStatus().name(),
			order.getCreatedAt(),
			productName);
	}

	private Slice<AuctionOrderDto> toDtoSlice(Slice<AuctionOrder> orderSlice) {
		List<AuctionOrder> orders = orderSlice.getContent();
		if (orders.isEmpty()) {
			return new SliceImpl<>(Collections.emptyList(), orderSlice.getPageable(), orderSlice.hasNext());
		}

		Set<Long> auctionIds = orders.stream().map(AuctionOrder::getAuctionId).collect(Collectors.toSet());
		Map<Long, Auction> auctionMap = auctionRepository.findAllById(auctionIds).stream()
			.collect(Collectors.toMap(Auction::getId, Function.identity()));

		Set<Long> productIds = auctionMap.values().stream().map(Auction::getProductId).collect(Collectors.toSet());
		Map<Long, String> productNameMap = productSearchClient.getProducts(productIds).values().stream()
			.collect(Collectors.toMap(product -> product.id(), product -> product.name(), (a, b) -> a));

		List<AuctionOrderDto> dtoList = orders.stream()
			.map(order -> from(order, auctionMap, productNameMap))
			.toList();

		return new SliceImpl<>(dtoList, orderSlice.getPageable(), orderSlice.hasNext());
	}

	private AuctionOrderDto from(AuctionOrder order, Map<Long, Auction> auctionMap, Map<Long, String> productNameMap) {
		Auction auction = auctionMap.get(order.getAuctionId());
		String productName = "Unknown Product";
		if (auction != null) {
			productName = productNameMap.getOrDefault(auction.getProductId(), "Unknown Product");
		}

		return new AuctionOrderDto(
			order.getId(),
			order.getAuctionId(),
			order.getSellerId(),
			order.getBidderId(),
			order.getFinalPrice(),
			order.getStatus().name(),
			order.getCreatedAt(),
			productName
		);
	}
}
