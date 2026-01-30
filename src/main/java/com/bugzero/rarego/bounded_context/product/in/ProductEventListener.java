package com.bugzero.rarego.bounded_context.product.in;

import static org.springframework.transaction.annotation.Propagation.*;
import static org.springframework.transaction.event.TransactionPhase.*;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.bounded_context.product.app.ProductFacade;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductEventListener {
	private final ProductFacade  productFacade;

	@TransactionalEventListener(phase = AFTER_COMMIT)
	@Transactional(propagation = REQUIRES_NEW)
	public void onMemberCreated(MemberJoinedEvent event) {
		productFacade.syncMember(event.memberDto());
	}

	@TransactionalEventListener(phase = AFTER_COMMIT)
	@Transactional(propagation = REQUIRES_NEW)
	public void onMemberUpdated(MemberUpdatedEvent event) {
		productFacade.syncMember(event.memberDto());
	}
}
