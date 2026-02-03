package com.bugzero.rarego.boundedContext.product.in;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bugzero.rarego.boundedContext.product.app.ProductImageS3UseCase;
import com.bugzero.rarego.shared.product.event.S3ImageConfirmEvent;
import com.bugzero.rarego.shared.product.event.S3ImageDeleteEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class S3ImageEventListener {

	private final ProductImageS3UseCase productImageS3UseCase;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 커밋 후 실행
	public void handleS3DeleteEvent(S3ImageDeleteEvent event) {
		productImageS3UseCase.deleteS3Image(event.paths());
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 커밋 후 실행
	public void handleS3ConfirmEvent(S3ImageConfirmEvent event) {
		productImageS3UseCase.confirmImages(event.paths());
	}

}
