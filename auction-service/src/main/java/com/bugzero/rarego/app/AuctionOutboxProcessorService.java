package com.bugzero.rarego.app;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.AuctionOutbox;
import com.bugzero.rarego.domain.AuctionOutboxStatus;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.AuctionOutboxRepository;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionOutboxProcessorService {

	private final AuctionOutboxRepository outboxRepository;
	private final ApplicationEventPublisher eventPublisher;

	private static final int MAX_RETRY = 3;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void process(Long outboxId) {
		AuctionOutbox outbox = outboxRepository.findById(outboxId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_OUTBOX_NOT_FOUND));

		// 1. 페이로드 검증
		Map<String, Object> payload = validatePayload(outbox);

		Long auctionId = ((Number)payload.get("auctionId")).longValue();

		log.debug(
			"아웃박스 처리 시작: id={}, auctionId={}, type={}, status={}",
			outboxId, auctionId, outbox.getType(), outbox.getStatus()
		);

		if (outbox.getStatus() == AuctionOutboxStatus.SENT) {
			log.debug("아웃박스 이미 처리됨 (중복 처리 방지): id={}", outboxId);
			return;
		}

		try {
			// 2. DB 상태 먼저 업데이트 (중복 발행 방지)
			outbox.markSent();
			outboxRepository.save(outbox);

			// 3. 이벤트 발행
			publishAuctionEndedEvent(payload);

			log.info(
				"아웃박스 처리 성공: id={}, auctionId={}, type={}",
				outboxId, auctionId, outbox.getType()
			);

		} catch (CustomException e) {
			// 복구 불가능한 예외 (페이로드 오류)
			log.error("아웃박스 검증 실패 (복구 불가): id={}, errorType={}", outboxId, e.getErrorType());
			outbox.markFailedPermanently(e.getErrorType().name(), MAX_RETRY);
			outboxRepository.save(outbox);

		} catch (Exception e) {
			// 복구 가능한 예외 (네트워크 등)
			log.warn("아웃박스 처리 실패 (재시도 가능): id={}, error={}", outboxId, e.getMessage());
			outbox.markFailedTransient(e.getMessage(), MAX_RETRY);
			outboxRepository.save(outbox);

			if (outbox.getStatus() == AuctionOutboxStatus.FAILED) {
				log.warn("아웃박스 최대 재시도 횟수 도달: id={}, lastError={}", outboxId, outbox.getLastError());
			}
		}
	}

	private Map<String, Object> validatePayload(AuctionOutbox outbox) {
		Map<String, Object> payload = outbox.getPayloadAsMap();

		if (payload == null) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}

		if (!payload.containsKey("auctionId") || payload.get("auctionId") == null) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}
		if (!payload.containsKey("bidderId") || payload.get("bidderId") == null) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}
		if (!payload.containsKey("bidAmount") || payload.get("bidAmount") == null) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}
		if (!payload.containsKey("productId") || payload.get("productId") == null) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}

		if (!(payload.get("auctionId") instanceof Number)) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}
		if (!(payload.get("bidderId") instanceof Number)) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}
		if (!(payload.get("bidAmount") instanceof Number)) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}
		if (!(payload.get("productId") instanceof Number)) {
			throw new CustomException(ErrorType.INVALID_OUTBOX_PAYLOAD);
		}

		return payload;
	}

	private void publishAuctionEndedEvent(Map<String, Object> payload) {
		Long auctionId = ((Number)payload.get("auctionId")).longValue();
		Long bidderId = ((Number)payload.get("bidderId")).longValue();
		Integer bidAmount = ((Number)payload.get("bidAmount")).intValue();
		Long productId = ((Number)payload.get("productId")).longValue();

		eventPublisher.publishEvent(new AuctionEndedEvent(
			auctionId,
			bidderId,
			bidAmount,
			productId
		));

		log.debug(
			"경매 종료 이벤트 발행: auctionId={}, bidderId={}, bidAmount={}, productId={}",
			auctionId, bidderId, bidAmount, productId
		);
	}
}
