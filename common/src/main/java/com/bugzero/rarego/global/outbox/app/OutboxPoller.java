package com.bugzero.rarego.global.outbox.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

	private final OutboxUseCase outboxUseCase;


	@Scheduled(fixedDelayString = "${outbox.poller.interval-ms:10000}")
	public void pollAndPublish() {
		//조회는 별도 트랜잭션 없이 진행 (조회용 메서드 호출)
		List<Long> pendingEvents = outboxUseCase.findPendingEventIds();

		if (pendingEvents.isEmpty()) return;

		log.info("[Outbox] {} 개의 이벤트를 발행합니다.", pendingEvents.size());

		for (Long eventId : pendingEvents) {
			// 2. 개별 이벤트마다 독립적인 트랜잭션으로 전송 및 상태 업데이트 진행
			outboxUseCase.processAndPublish(eventId);
		}
	}


	@Scheduled(cron = "${outbox.cleanup.cron:0 0 3 * * *}")
	public void cleanupOldEvents() {
		LocalDateTime threshold = LocalDateTime.now().minusDays(7);
		int batchSize = 100; // 소규모 배치 (인박스와 동일)
		long totalDeleted = 0;

		log.info("[OutboxCleanup] {} 이전의 전송 완료된 데이터 정리를 시작합니다.", threshold);

		// 최대 50번 시도 (총 5,000건) - 무한 루프 방지
		for (int i = 0; i < 50; i++) {
			try {
				int deletedCount = outboxUseCase.deleteSentEventsBatch(threshold, batchSize);
				totalDeleted += deletedCount;

				if (deletedCount < batchSize) {
					break; // 더 이상 지울 데이터가 없음
				}

				Thread.sleep(50); // DB 숨 고르기

			} catch (Exception e) {
				log.error("[OutboxCleanup] {}번째 배치 처리 중 오류 발생. 다음 배치를 계속합니다.", i + 1, e);
			}
		}

		if (totalDeleted > 0) {
			log.info("[OutboxCleanup] 정리 완료. 총 {}건 삭제되었습니다.", totalDeleted);
		}
	}

}
