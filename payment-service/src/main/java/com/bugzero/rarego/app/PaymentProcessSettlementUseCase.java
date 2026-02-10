package com.bugzero.rarego.app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Settlement;
import com.bugzero.rarego.domain.SettlementStatus;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;
import com.bugzero.rarego.shared.payment.dto.SettlementResponseDto;
import com.bugzero.rarego.out.SettlementRepository;
import com.bugzero.rarego.global.event.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessSettlementUseCase {
	private final SettlementRepository settlementRepository;
	private final PaymentSettlementProcessor paymentSettlementProcessor;
	private final EventPublisher eventPublisher;

	@Value("${custom.payment.settlement.holdDays:7}")
	private int settlementHoldDays;

	@Transactional
	public int processSettlements(int limit) {
		// 7일 경과한 정산만 처리
		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(settlementHoldDays);

		List<Settlement> settlements = settlementRepository.findSettlementsForBatch(
			SettlementStatus.READY, cutoffDate, limit);

		if (settlements.isEmpty()) {
			// 정산할 게 없어도, 혹시 이전에 남겨진 수수료가 있다면 처리
			eventPublisher.publish(SettlementFinishedEvent.of(List.of()));
			return 0;
		}

		List<SettlementResponseDto> successSettlements = new ArrayList<>();

		for (Settlement settlement : settlements) {
			try {
				if (paymentSettlementProcessor.processSellerDeposit(settlement)) {
					successSettlements.add(new SettlementResponseDto(
						settlement.getId(),
						settlement.getAuctionId(),
						settlement.getSeller().getId(),
						settlement.getSalesAmount(),
						settlement.getFeeAmount(),
						settlement.getSettlementAmount(),
						settlement.getStatus().name(), // Enum -> String 변환
						settlement.getCreatedAt()
					));
				}
			} catch (Exception e) {
				boolean isFinalFailure = settlement.fail();

				if (isFinalFailure) {
					log.error("정산 최종 실패 - ID: {}, 원인: {}. 수동 처리 필요", settlement.getId(), e.getMessage(), e);
				} else {
					log.warn("일시적 정산 실패 - ID: {}, 원인: {}. 다음 배치에서 재시도", settlement.getId(), e.getMessage());
				}
			}
		}

		eventPublisher.publish(SettlementFinishedEvent.of(successSettlements));

		return successSettlements.size();
	}
}
