package com.bugzero.rarego.bounded_context.payment.in;

import static org.springframework.transaction.annotation.Propagation.*;
import static org.springframework.transaction.event.TransactionPhase.*;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.bounded_context.payment.app.PaymentFacade;
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
}
