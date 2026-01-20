package com.bugzero.rarego.boundedContext.auction.in;

import static org.springframework.transaction.annotation.Propagation.*;
import static org.springframework.transaction.event.TransactionPhase.*;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionUpdatedEvent;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 경매 생성/수정 시 자동으로 정산 작업을 예약
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {

	private final AuctionScheduler scheduler;
	private final AuctionFacade auctionFacade;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAuctionCreated(AuctionCreatedEvent event) {
		try {
			if (event == null || event.auctionId() == null || event.endTime() == null) {
				log.error("유효하지 않은 AuctionCreatedEvent: {}", event);
				return;
			}

			log.info("경매 생성 이벤트 수신 - auctionId: {}", event.auctionId());
			scheduler.scheduleSettlement(event.auctionId(), event.endTime());

		} catch (Exception e) {
			log.error("경매 {} 생성 이벤트 처리 실패", event.auctionId(), e);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAuctionUpdated(AuctionUpdatedEvent event) {
		try {
			if (event == null || event.auctionId() == null || event.newEndTime() == null) {
				log.error("유효하지 않은 AuctionUpdatedEvent: {}", event);
				return;
			}

			log.info("경매 수정 이벤트 수신 - auctionId: {}", event.auctionId());
			scheduler.cancelSchedule(event.auctionId());
			scheduler.scheduleSettlement(event.auctionId(), event.newEndTime());

		} catch (Exception e) {
			log.error("경매 {} 수정 이벤트 처리 실패", event.auctionId(), e);
		}
	}

	@TransactionalEventListener(phase = AFTER_COMMIT)
	@Transactional(propagation = REQUIRES_NEW)
	public void onMemberCreated(MemberJoinedEvent event) {
		auctionFacade.syncMember(event.memberDto());
	}

}
