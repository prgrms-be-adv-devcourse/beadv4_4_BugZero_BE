package com.bugzero.rarego.app;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.AuctionOutbox;
import com.bugzero.rarego.domain.AuctionOutboxStatus;
import com.bugzero.rarego.out.AuctionOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionOutboxProcessor {

	private final AuctionOutboxRepository outboxRepository;
	private final AuctionOutboxProcessorService processorService;

	private static final int MAX_RETRY = 3;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int retryPending() {
		// 미처리 아웃박스 조회 (FOR UPDATE SKIP LOCKED 적용)
		// → 다른 트랜잭션에서 이미 처리 중인 행은 스킵
		List<AuctionOutbox> pendings = outboxRepository
			.findAllByStatusAndRetryCountLessThanWithLock(
				AuctionOutboxStatus.PENDING, MAX_RETRY
			);

		log.debug("미처리 아웃박스 조회: count={}", pendings.size());

		// 각 아웃박스마다 처리 시도
		int successCount = 0;
		for (AuctionOutbox outbox : pendings) {
			try {
				processorService.process(outbox.getId());
				successCount++;

			} catch (Exception e) {
				log.error(
					"아웃박스 개별 재시도 실패: id={}, error={}",
					outbox.getId(), e.getMessage()
				);
			}
		}

		log.debug("아웃박스 배치 재시도 완료: total={}, success={}", pendings.size(), successCount);
		return successCount;
	}
}
