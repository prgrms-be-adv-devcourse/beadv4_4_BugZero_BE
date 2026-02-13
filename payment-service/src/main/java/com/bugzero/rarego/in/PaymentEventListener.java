package com.bugzero.rarego.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.app.PaymentFacade;
import com.bugzero.rarego.app.PaymentSettlementProcessor;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {
	private final PaymentFacade paymentFacade;
	private final PaymentSettlementProcessor paymentSettlementProcessor;

	// 경매 종료 이벤트 수신 → 보증금 반환 처리
	// TODO: @Retryable 대신 kafka errorhandler + DLQ 패턴 도입 고민
	@KafkaListener(topics = "auction-ended", groupId = "payment-service-group")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleAuctionEnded(AuctionEndedEvent event) {
		try {
			log.info("카프카 메시지 수신: 경매 종료 - auctionId={}, winnerId={}",
				event.auctionId(), event.winnerId());

			paymentFacade.releaseDeposits(event.auctionId(), event.winnerId());

			log.info("보증금 반환 처리 완료 - auctionId={}", event.auctionId());

		} catch (Exception e) {
			log.error("보증금 반환 처리 실패 - auctionId: {}", event.auctionId(), e);
			// auto-offset-reset=earliest 설정으로 인해 재시작 시 재처리됨
			throw e;
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSettlementFinished(SettlementFinishedEvent event) {
		try {
			paymentSettlementProcessor.processFees(1000);
		} catch (Exception e) {
			log.error("수수료 징수 중 에러 발생 (다음 배치에서 처리됨)", e);
		}
	}
}
