package com.bugzero.rarego.in;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.outbox.app.OutboxUseCase;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.AuctionBookmarkRepository;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.out.es.ProductSearchClient;
import com.bugzero.rarego.shared.auction.event.AuctionStartedEvent;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuctionStartScheduler {

	private static final int START_BATCH_SIZE = 200;

	private final AuctionRepository auctionRepository;
	private final AuctionBookmarkRepository auctionBookmarkRepository;
	private final OutboxUseCase outboxUseCase;
	private final ProductSearchClient productSearchClient;
	private final AuctionStartScheduler self;

	public AuctionStartScheduler(
		AuctionRepository auctionRepository,
		AuctionBookmarkRepository auctionBookmarkRepository,
		OutboxUseCase outboxUseCase,
		ProductSearchClient productSearchClient,
		@Lazy AuctionStartScheduler self
	) {
		this.auctionRepository = auctionRepository;
		this.auctionBookmarkRepository = auctionBookmarkRepository;
		this.outboxUseCase = outboxUseCase;
		this.productSearchClient = productSearchClient;
		this.self = self;
	}

	@Scheduled(cron = "0 * * * * *")
	public void autoStartAuctions() {
		LocalDateTime now = LocalDateTime.now();
		int totalCandidates = 0;

		while (true) {
			Page<Auction> pendingPage = auctionRepository.findAllByStatusAndStartTimeBefore(
				AuctionStatus.SCHEDULED,
				now,
				PageRequest.of(0, START_BATCH_SIZE)
			);
			List<Auction> pendingAuctions = pendingPage.getContent();
			if (pendingAuctions.isEmpty()) {
				break;
			}

			totalCandidates += pendingAuctions.size();

			for (Auction auction : pendingAuctions) {
				try {
					self.processStart(auction.getId());
				} catch (Exception e) {
					log.error("경매 ID {} 시작 처리 중 오류 발생", auction.getId(), e);
				}
			}
		}

		if (totalCandidates > 0) {
			log.info("경매 자동 시작 스케줄러 실행: {}건 시작 처리", totalCandidates);
		}
	}

	@Transactional
	public void processStart(Long auctionId) {
		Auction auction = auctionRepository.findByIdWithLock(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

		if (auction.getStatus() != AuctionStatus.SCHEDULED) {
			throw new CustomException(ErrorType.AUCTION_NOT_SCHEDULED);
		}

		auction.start();

		List<Long> bookmarkedMemberIds = auctionBookmarkRepository
			.findMemberIdsByAuctionId(auction.getId());

		String productName = getProductName(auction.getProductId());

		AuctionStartedEvent event = new AuctionStartedEvent(
			auction.getId(),
			auction.getProductId(),
			auction.getStartTime(),
			productName,
			bookmarkedMemberIds
		);
		outboxUseCase.saveOutbox(event);

		log.info("경매 시작 처리 완료: auctionId={}, bookmarkedCount={}",
			auction.getId(), bookmarkedMemberIds.size());
	}

	private String getProductName(Long productId) {
		try {
			return productSearchClient.getProduct(productId)
				.map(ProductAuctionResponseDto::name)
				.orElse("Unknown Product");
		} catch (Exception e) {
			log.warn("상품명 조회 실패, 기본값 사용. productId={}", productId, e);
			return "Unknown Product";
		}
	}
}
