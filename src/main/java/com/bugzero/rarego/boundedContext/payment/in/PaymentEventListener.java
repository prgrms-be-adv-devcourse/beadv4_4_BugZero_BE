package com.bugzero.rarego.boundedContext.payment.in;

import static org.springframework.transaction.annotation.Propagation.*;
import static org.springframework.transaction.event.TransactionPhase.*;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;
import com.bugzero.rarego.boundedContext.payment.app.PaymentSettlementProcessor;
import com.bugzero.rarego.boundedContext.payment.event.SettlementFinishedEvent;
import com.bugzero.rarego.shared.auction.event.AuctionEndedEvent;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {
	private final PaymentFacade paymentFacade;
	private final PaymentSettlementProcessor paymentSettlementProcessor;

	// TODO: Spring Retry 추가 검토 (@Retryable)
	@TransactionalEventListener(phase = AFTER_COMMIT)
	@Transactional(propagation = REQUIRES_NEW)
	public void handle(AuctionEndedEvent event) {
		log.info("경매 종료 이벤트 수신: auctionId={}, winnerId={}", event.auctionId(), event.winnerId());
		paymentFacade.releaseDeposits(event.auctionId(), event.winnerId());
	}

	@TransactionalEventListener(phase = AFTER_COMMIT)
	@Transactional(propagation = REQUIRES_NEW)
	public void onMemberCreated(MemberJoinedEvent event) {
		paymentFacade.syncMember(event.memberDto());
	}

	@TransactionalEventListener(phase = AFTER_COMMIT)
	@Transactional(propagation = REQUIRES_NEW)
	public void onMemberUpdated(MemberUpdatedEvent event) {
		paymentFacade.syncMember(event.memberDto());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSettlementFinished(SettlementFinishedEvent event) {
		try {
			// 기존과 동일하게 처리 (REQUIRES_NEW가 있어서 새 트랜잭션으로 돔)
			paymentSettlementProcessor.processFees(1000);
		} catch (Exception e) {
			log.error("수수료 징수 중 에러 발생 (다음 배치에서 처리됨)", e);
		}
	}
}
