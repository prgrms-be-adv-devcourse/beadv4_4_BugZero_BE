package com.bugzero.rarego.app;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.domain.AuctionOrder;
import com.bugzero.rarego.domain.AuctionOutbox;
import com.bugzero.rarego.domain.Bid;
import com.bugzero.rarego.domain.event.AuctionFailedEvent;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.AuctionOrderRepository;
import com.bugzero.rarego.out.AuctionOutboxRepository;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.out.BidRepository;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionSettlementSupport {

	private final AuctionRepository auctionRepository;
	private final BidRepository bidRepository;
	private final AuctionOrderRepository auctionOrderRepository;
	private final AuctionOutboxRepository auctionOutboxRepository;
	private final AuctionOutboxProcessorService auctionOutboxProcessorService;
	private final ApplicationEventPublisher eventPublisher;

	private static final int BATCH_SIZE = 100;

	@Transactional(readOnly = true)
	public List<Auction> findExpiredAuctions(LocalDateTime now) {
		return auctionRepository.findExpiredInProgressAuctionsWithLock(
			now, PageRequest.of(0, BATCH_SIZE)
		);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processSettlement(Long auctionId) {
		Auction auction = auctionRepository.findByIdWithLock(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

		if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
			throw new CustomException(ErrorType.AUCTION_NOT_FOUND_OR_ALREADY_SETTLED);
		}

		if (auction.getEndTime().isAfter(LocalDateTime.now())) {
			throw new CustomException(ErrorType.AUCTION_NOT_FINISHED);
		}

		// 낙찰/유찰 처리
		if (bidRepository.existsByAuctionId(auction.getId())) {
			handleSuccess(auction);  // 외부 이벤트 → 아웃박스
		} else {
			handleFail(auction);     // 내부 이벤트 → 즉시 발행
		}
	}

	private void handleSuccess(Auction auction) {
		Bid winningBid = bidRepository.findTopByAuctionIdOrderByBidAmountDescBidTimeAsc(auction.getId())
			.orElseThrow(() -> new CustomException(ErrorType.BID_NOT_FOUND));

		auction.end();
		auctionRepository.save(auction);

		auctionOrderRepository.save(
			AuctionOrder.builder()
				.auctionId(auction.getId())
				.sellerId(auction.getSellerId())
				.bidderId(winningBid.getBidderId())
				.finalPrice(winningBid.getBidAmount())
				.build()
		);

		// 아웃박스에 저장 (외부 이벤트)
		saveOutboxAndSync(
			AuctionOutbox.createAuctionEnded(
				auction.getId(),
				winningBid.getBidderId(),
				winningBid.getBidAmount(),
				auction.getProductId()
			)
		);

		log.info(
			"낙찰 정산 완료: auctionId={}, bidderId={}, bidAmount={}",
			auction.getId(), winningBid.getBidderId(), winningBid.getBidAmount()
		);
	}

	private void handleFail(Auction auction) {
		auction.end();
		auctionRepository.save(auction);

		eventPublisher.publishEvent(
			new AuctionFailedEvent(
				auction.getId(),
				auction.getProductId()
			)
		);

		log.info(
			"유찰 정산 완료: auctionId={}, productId={}",
			auction.getId(), auction.getProductId()
		);
	}

	private void saveOutboxAndSync(AuctionOutbox outbox) {
		AuctionOutbox saved = auctionOutboxRepository.save(outbox);

		Map<String, Object> payload = saved.getPayloadAsMap();
		Long auctionId = ((Number)payload.get("auctionId")).longValue();

		log.debug(
			"아웃박스 생성: outboxId={}, auctionId={}, type={}",
			saved.getId(), auctionId, saved.getType()
		);

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						try {
							auctionOutboxProcessorService.process(saved.getId());

							log.info(
								"아웃박스 처리 성공: outboxId={}, auctionId={}, type={}",
								saved.getId(), auctionId, saved.getType()
							);

						} catch (Exception e) {
							log.error(
								"아웃박스 처리 실패 (커밋 후 콜백): " +
									"outboxId={}, auctionId={}, type={}, error={}",
								saved.getId(), auctionId,
								saved.getType(), e.getMessage(), e
							);
							// 스케줄러가 재시도함
						}
					}
				}
			);
		}
	}

	// 헬퍼 메서드들

	@Transactional(readOnly = true)
	public boolean hasBids(Long auctionId) {
		return bidRepository.existsByAuctionId(auctionId);
	}

	@Transactional(readOnly = true)
	public Bid findWinningBid(Long auctionId) {
		return bidRepository.findTopByAuctionIdOrderByBidAmountDescBidTimeAsc(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.BID_NOT_FOUND));
	}
}
