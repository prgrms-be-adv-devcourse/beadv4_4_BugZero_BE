package com.bugzero.rarego.boundedContext.payment.app;

import com.bugzero.rarego.shared.payment.dto.DepositHoldRequest;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentFacade {

	private final PaymentHoldDepositUseCase paymentHoldDepositUseCase;

	/**
	 * 보증금 홀딩
	 */
	@Transactional
	public DepositHoldResponse holdDeposit(DepositHoldRequest request) {
		return paymentHoldDepositUseCase.holdDeposit(request);
	}
}