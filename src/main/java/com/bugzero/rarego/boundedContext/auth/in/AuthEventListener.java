package com.bugzero.rarego.boundedContext.auth.in;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.boundedContext.auth.app.AuthPromoteSellerUseCase;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.event.MemberBecameSellerEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthEventListener {
	private final AuthPromoteSellerUseCase authPromoteSellerUseCase;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(MemberBecameSellerEvent event) {
		if (event == null || event.getPublicId() == null || event.getPublicId().isBlank()) {
			throw new CustomException(ErrorType.INVALID_INPUT);
		}
		authPromoteSellerUseCase.promoteSeller(event.getPublicId());
	}
}
