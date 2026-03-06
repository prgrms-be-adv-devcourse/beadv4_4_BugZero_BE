package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Deposit;
import com.bugzero.rarego.domain.DepositHoldSagaStep;
import com.bugzero.rarego.domain.DepositStatus;
import com.bugzero.rarego.domain.PaymentMember;
import com.bugzero.rarego.domain.PaymentSagaType;
import com.bugzero.rarego.domain.PaymentTransaction;
import com.bugzero.rarego.domain.ReferenceType;
import com.bugzero.rarego.domain.Wallet;
import com.bugzero.rarego.domain.WalletTransactionType;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.DepositRepository;
import com.bugzero.rarego.out.PaymentTransactionRepository;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentHoldDepositUseCase {
	private final DepositRepository depositRepository;
	private final PaymentTransactionRepository transactionRepository;
	private final PaymentSupport paymentSupport;
	private final PaymentSagaTracker sagaTracker;

	@Transactional
	public DepositHoldResponseDto holdDeposit(DepositHoldRequestDto request) {
		// publicId → memberId 변환
		PaymentMember member = paymentSupport.findMemberByPublicId(request.memberPublicId());
		Long memberId = member.getId();
		String sagaBusinessKey = String.format("%d:%d", request.auctionId(), memberId);
		DepositHoldSagaStep failedStep = null;

		try {
			Deposit existingDeposit = depositRepository.findByMemberIdAndAuctionId(memberId, request.auctionId())
				.orElse(null);

			if (existingDeposit != null) {
				if (existingDeposit.getStatus() == DepositStatus.HOLD) {
					return buildResponse(existingDeposit, false);
				}
				if (existingDeposit.getStatus() != DepositStatus.RELEASED) {
					throw new CustomException(ErrorType.INVALID_DEPOSIT_STATUS);
				}

				sagaTracker.startOrResume(PaymentSagaType.DEPOSIT_HOLD, sagaBusinessKey, DepositHoldSagaStep.INITIATED);
				failedStep = DepositHoldSagaStep.INITIATED;
				DepositHoldResponseDto response = executeReHold(existingDeposit, member, request.amount());
				failedStep = DepositHoldSagaStep.HOLD_LOCAL_DONE;
				safeMarkStep(sagaBusinessKey, DepositHoldSagaStep.HOLD_LOCAL_DONE);
				safeMarkCheckpoint(sagaBusinessKey, DepositHoldSagaStep.HOLD_LOCAL_DONE);
				safeMarkStep(sagaBusinessKey, DepositHoldSagaStep.WAITING_BID_RESULT);
				safeMarkCheckpoint(sagaBusinessKey, DepositHoldSagaStep.WAITING_BID_RESULT);
				return response;
			}

			sagaTracker.startOrResume(PaymentSagaType.DEPOSIT_HOLD, sagaBusinessKey, DepositHoldSagaStep.INITIATED);
			failedStep = DepositHoldSagaStep.INITIATED;
			DepositHoldResponseDto response = executeHold(member, request);
			failedStep = DepositHoldSagaStep.HOLD_LOCAL_DONE;
			safeMarkStep(sagaBusinessKey, DepositHoldSagaStep.HOLD_LOCAL_DONE);
			safeMarkCheckpoint(sagaBusinessKey, DepositHoldSagaStep.HOLD_LOCAL_DONE);
			safeMarkStep(sagaBusinessKey, DepositHoldSagaStep.WAITING_BID_RESULT);
			safeMarkCheckpoint(sagaBusinessKey, DepositHoldSagaStep.WAITING_BID_RESULT);
			return response;
		} catch (Exception ex) {
			if (failedStep != null) {
				try {
					sagaTracker.markFailed(PaymentSagaType.DEPOSIT_HOLD, sagaBusinessKey, failedStep, ex);
				} catch (Exception sagaEx) {
					// 보증금 홀드 실패보다 Saga 기록 실패가 응답을 덮어쓰지 않도록 한다.
				}
			}
			throw ex;
		}
	}

	private void safeMarkStep(String sagaBusinessKey, DepositHoldSagaStep step) {
		try {
			sagaTracker.markStep(PaymentSagaType.DEPOSIT_HOLD, sagaBusinessKey, step);
		} catch (Exception e) {
			log.error("보증금 홀드 Saga 단계 기록 실패: businessKey={}, step={}, error={}",
				sagaBusinessKey, step, e.getMessage());
		}
	}

	private void safeMarkCheckpoint(String sagaBusinessKey, DepositHoldSagaStep step) {
		try {
			sagaTracker.markCheckpoint(PaymentSagaType.DEPOSIT_HOLD, sagaBusinessKey, step);
		} catch (Exception e) {
			log.error("보증금 홀드 Saga 체크포인트 기록 실패: businessKey={}, step={}, error={}",
				sagaBusinessKey, step, e.getMessage());
		}
	}

	private DepositHoldResponseDto executeReHold(Deposit deposit, PaymentMember member, int amount) {
		// 1. 지갑 잔액 확인 및 홀딩 (Wallet 업데이트)
		Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(member.getId());
		wallet.hold(amount);

		// 2. Deposit 상태 변경
		deposit.reHold(amount);

		// 3. 이력 기록
		PaymentTransaction transaction = PaymentTransaction.builder()
			.member(member)
			.wallet(wallet)
			.transactionType(WalletTransactionType.DEPOSIT_HOLD)
			.balanceDelta(0)
			.holdingDelta(amount)
			.balanceAfter(wallet.getBalance())
			.referenceType(ReferenceType.DEPOSIT)
			.referenceId(deposit.getId())
			.build();
		transactionRepository.save(transaction);

		return buildResponse(deposit, true);
	}

	private DepositHoldResponseDto executeHold(PaymentMember member, DepositHoldRequestDto request) {
		// 2. 잔액 검증 & Wallet 업데이트
		Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(member.getId());
		wallet.hold(request.amount());

		// 3. Deposit 엔티티 생성 및 저장
		Deposit deposit = Deposit.create(member, request.auctionId(), request.amount());
		depositRepository.save(deposit);

		// 4. 이력 기록
		PaymentTransaction transaction = PaymentTransaction.builder()
			.member(member)
			.wallet(wallet)
			.transactionType(WalletTransactionType.DEPOSIT_HOLD)
			.balanceDelta(0)
			.holdingDelta(request.amount())
			.balanceAfter(wallet.getBalance())
			.referenceType(ReferenceType.DEPOSIT)
			.referenceId(deposit.getId())
			.build();
		transactionRepository.save(transaction);

		return buildResponse(deposit, true);
	}

	private DepositHoldResponseDto buildResponse(Deposit deposit, boolean holdApplied) {
		return new DepositHoldResponseDto(
			deposit.getId(),
			deposit.getAuctionId(),
			deposit.getAmount(),
			deposit.getStatus().name(),
			deposit.getCreatedAt(),
			holdApplied
		);
	}
}
