package com.bugzero.rarego.app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.bugzero.rarego.domain.Deposit;
import com.bugzero.rarego.domain.DepositHoldSagaStep;
import com.bugzero.rarego.domain.DepositStatus;
import com.bugzero.rarego.domain.PaymentSagaExecution;
import com.bugzero.rarego.domain.PaymentSagaExecutionStatus;
import com.bugzero.rarego.domain.PaymentSagaType;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.DepositRepository;
import com.bugzero.rarego.out.PaymentSagaExecutionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDepositHoldSagaRecoveryUseCase {
	private static final int DEFAULT_BATCH_LIMIT = 50;

	private final PaymentSagaExecutionRepository sagaRepository;
	private final DepositRepository depositRepository;
	private final PaymentSagaTracker sagaTracker;
	private final PaymentReleaseDepositUseCase paymentReleaseDepositUseCase;
	private final PlatformTransactionManager transactionManager;

	@Value("${payment.saga.deposit-hold-recovery.batch-size:" + DEFAULT_BATCH_LIMIT + "}")
	private int batchSize;

	@Value("${payment.saga.deposit-hold-recovery.wait-timeout-seconds:300}")
	private long waitTimeoutSeconds;

	public void recoverExpiredDepositHolds() {
		LocalDateTime now = LocalDateTime.now();
		List<PaymentSagaExecution> failedTargets = sagaRepository.findRetryTargetsForBatch(
			PaymentSagaType.DEPOSIT_HOLD,
			PaymentSagaExecutionStatus.FAILED,
			now,
			batchSize
		);
		List<PaymentSagaExecution> staleTargets = sagaRepository.findStaleInProgressTargetsForBatch(
			PaymentSagaType.DEPOSIT_HOLD,
			PaymentSagaExecutionStatus.IN_PROGRESS,
			now.minusSeconds(waitTimeoutSeconds),
			batchSize
		);
		List<PaymentSagaExecution> targets = mergeTargets(failedTargets, staleTargets, batchSize);
		if (targets.isEmpty()) {
			return;
		}

		int success = 0;
		int failed = 0;
		for (PaymentSagaExecution target : targets) {
			try {
				runInNewTransaction(() -> recoverSingle(target.getBusinessKey()));
				success++;
			} catch (Exception e) {
				failed++;
				log.error("보증금 홀드 Saga 복구 실패: businessKey={}, error={}", target.getBusinessKey(), e.getMessage());
			}
		}
		log.info("보증금 홀드 Saga 복구 배치 완료: 대상={}, 성공={}, 실패={}", targets.size(), success, failed);
	}

	private void runInNewTransaction(Runnable task) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.executeWithoutResult(status -> task.run());
	}

	public void recoverSingle(String sagaBusinessKey) {
		PaymentSagaExecution saga = sagaRepository.findBySagaTypeAndBusinessKeyForUpdate(PaymentSagaType.DEPOSIT_HOLD,
				sagaBusinessKey)
			.orElseThrow(() -> new CustomException(ErrorType.DEPOSIT_NOT_FOUND));

		if (!isRecoverableStatus(saga) || !isDueForRecovery(saga)) {
			return;
		}

		DepositHoldSagaStep failedStep = resolveStepOrDefault(saga.getFailedStep(),
			DepositHoldSagaStep.WAITING_BID_RESULT);
		DepositHoldSagaStep checkpoint = resolveStepOrDefault(saga.getCheckpointStep(), DepositHoldSagaStep.INITIATED);
		Long[] businessKey = parseBusinessKey(sagaBusinessKey);
		Long auctionId = businessKey[0];
		Long memberId = businessKey[1];

		if (!hasReached(checkpoint, DepositHoldSagaStep.HOLD_LOCAL_DONE)) {
			if (!reconcileCheckpointWithDepositState(saga, auctionId, memberId)) {
				return;
			}
		}

		sagaTracker.startOrResume(saga, DepositHoldSagaStep.WAITING_BID_RESULT);

		try {
			boolean released = paymentReleaseDepositUseCase.releaseDepositByMemberIdForRecovery(auctionId, memberId);
			sagaTracker.markStep(saga, DepositHoldSagaStep.COMPENSATION_RELEASE_DONE);
			sagaTracker.markCheckpoint(saga, DepositHoldSagaStep.COMPENSATION_RELEASE_DONE);
			sagaTracker.markCompleted(saga, DepositHoldSagaStep.COMPLETED);
			log.info("보증금 홀드 Saga TTL 보상 완료: auctionId={}, memberId={}, released={}",
				auctionId, memberId, released);
		} catch (Exception ex) {
			sagaTracker.markFailed(saga, failedStep, ex);
			throw ex;
		}
	}

	private boolean reconcileCheckpointWithDepositState(PaymentSagaExecution saga, Long auctionId, Long memberId) {
		Deposit deposit = depositRepository.findByMemberIdAndAuctionId(memberId, auctionId).orElse(null);
		if (deposit == null) {
			log.warn("보증금 홀드 Saga 재개 스킵: checkpoint 이전이지만 보증금이 존재하지 않음. auctionId={}, memberId={}",
				auctionId, memberId);
			sagaTracker.markCompleted(saga, DepositHoldSagaStep.COMPLETED);
			return false;
		}

		if (deposit.getStatus() == DepositStatus.HOLD) {
			log.warn("보증금 홀드 Saga 재개 보정: checkpoint 이전이지만 보증금은 HOLD 상태입니다. auctionId={}, memberId={}",
				auctionId, memberId);
			sagaTracker.markStep(saga, DepositHoldSagaStep.HOLD_LOCAL_DONE);
			sagaTracker.markCheckpoint(saga, DepositHoldSagaStep.HOLD_LOCAL_DONE);
			return true;
		}

		if (deposit.getStatus() == DepositStatus.RELEASED
			|| deposit.getStatus() == DepositStatus.USED
			|| deposit.getStatus() == DepositStatus.FORFEITED) {
			log.info("보증금 홀드 Saga 재개 종료: checkpoint 이전이며 보증금 상태가 이미 종결됨. auctionId={}, memberId={}, status={}",
				auctionId, memberId, deposit.getStatus());
			sagaTracker.markCompleted(saga, DepositHoldSagaStep.COMPLETED);
			return false;
		}

		return false;
	}

	private boolean hasReached(DepositHoldSagaStep checkpoint, DepositHoldSagaStep target) {
		return checkpoint.ordinal() >= target.ordinal();
	}

	private boolean isRecoverableStatus(PaymentSagaExecution saga) {
		return saga.getStatus() == PaymentSagaExecutionStatus.FAILED
			|| saga.getStatus() == PaymentSagaExecutionStatus.IN_PROGRESS;
	}

	private boolean isDueForRecovery(PaymentSagaExecution saga) {
		LocalDateTime now = LocalDateTime.now();
		if (saga.getStatus() == PaymentSagaExecutionStatus.FAILED) {
			return saga.isRetryable() && saga.getNextRetryAt() != null && !saga.getNextRetryAt().isAfter(now);
		}
		if (saga.getStatus() == PaymentSagaExecutionStatus.IN_PROGRESS) {
			return saga.getLastAttemptAt() != null
				&& !saga.getLastAttemptAt().isAfter(now.minusSeconds(waitTimeoutSeconds));
		}
		return false;
	}

	private List<PaymentSagaExecution> mergeTargets(List<PaymentSagaExecution> failedTargets,
		List<PaymentSagaExecution> staleTargets, int limit) {
		Map<String, PaymentSagaExecution> merged = new LinkedHashMap<>();
		for (PaymentSagaExecution target : failedTargets) {
			merged.put(target.getBusinessKey(), target);
			if (merged.size() >= limit) {
				return new ArrayList<>(merged.values());
			}
		}
		for (PaymentSagaExecution target : staleTargets) {
			merged.putIfAbsent(target.getBusinessKey(), target);
			if (merged.size() >= limit) {
				break;
			}
		}
		return new ArrayList<>(merged.values());
	}

	private DepositHoldSagaStep resolveStepOrDefault(String stepName, DepositHoldSagaStep defaultStep) {
		if (stepName == null || stepName.isBlank()) {
			return defaultStep;
		}
		for (DepositHoldSagaStep step : DepositHoldSagaStep.values()) {
			if (step.name().equals(stepName)) {
				return step;
			}
		}
		log.warn("유효하지 않은 보증금 홀드 Saga step 값을 기본값으로 대체합니다. stepName={}, defaultStep={}", stepName, defaultStep);
		return defaultStep;
	}

	private Long[] parseBusinessKey(String sagaBusinessKey) {
		String[] keyParts = sagaBusinessKey.split(":");
		if (keyParts.length != 2) {
			throw new CustomException(ErrorType.INVALID_INPUT);
		}
		return new Long[] {Long.parseLong(keyParts[0]), Long.parseLong(keyParts[1])};
	}
}
