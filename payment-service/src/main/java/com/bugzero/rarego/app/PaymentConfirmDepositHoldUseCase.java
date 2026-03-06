package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Deposit;
import com.bugzero.rarego.domain.DepositHoldSagaStep;
import com.bugzero.rarego.domain.DepositStatus;
import com.bugzero.rarego.domain.PaymentMember;
import com.bugzero.rarego.domain.PaymentSagaType;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.DepositRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentConfirmDepositHoldUseCase {
	private final PaymentSupport paymentSupport;
	private final DepositRepository depositRepository;
	private final PaymentSagaTracker sagaTracker;

	@Transactional
	public void confirm(Long auctionId, String memberPublicId) {
		PaymentMember member = paymentSupport.findMemberByPublicId(memberPublicId);
		Deposit deposit = depositRepository.findByMemberIdAndAuctionId(member.getId(), auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.DEPOSIT_NOT_FOUND));

		if (deposit.getStatus() != DepositStatus.HOLD) {
			throw new CustomException(ErrorType.INVALID_DEPOSIT_STATUS);
		}

		String businessKey = String.format("%d:%d", auctionId, member.getId());
		sagaTracker.markCheckpoint(PaymentSagaType.DEPOSIT_HOLD, businessKey, DepositHoldSagaStep.CONFIRMED);
		// CONFIRMED를 terminal step으로 남겨 late release에서 확인 가능한 신호로 사용한다.
		sagaTracker.markCompleted(PaymentSagaType.DEPOSIT_HOLD, businessKey, DepositHoldSagaStep.CONFIRMED);
		log.info("보증금 홀드 확정 완료: auctionId={}, memberId={}", auctionId, member.getId());
	}
}
