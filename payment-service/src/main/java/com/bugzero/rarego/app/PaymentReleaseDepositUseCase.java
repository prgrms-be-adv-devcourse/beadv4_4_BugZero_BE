package com.bugzero.rarego.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Deposit;
import com.bugzero.rarego.domain.DepositHoldSagaStep;
import com.bugzero.rarego.domain.DepositStatus;
import com.bugzero.rarego.domain.PaymentMember;
import com.bugzero.rarego.domain.PaymentSagaExecution;
import com.bugzero.rarego.domain.PaymentSagaExecutionStatus;
import com.bugzero.rarego.domain.PaymentSagaType;
import com.bugzero.rarego.domain.PaymentTransaction;
import com.bugzero.rarego.domain.ReferenceType;
import com.bugzero.rarego.domain.Wallet;
import com.bugzero.rarego.domain.WalletTransactionType;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.out.DepositRepository;
import com.bugzero.rarego.out.PaymentSagaExecutionRepository;
import com.bugzero.rarego.out.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentReleaseDepositUseCase {
	private final DepositRepository depositRepository;
	private final PaymentSupport paymentSupport;
	private final PaymentTransactionRepository transactionRepository;
	private final PaymentSagaExecutionRepository sagaExecutionRepository;
	private final PaymentSagaTracker sagaTracker;

	@Transactional
	public void releaseDeposits(Long auctionId, Long winnerId) {
		List<Deposit> depositsToRelease = findDepositsToRelease(auctionId, winnerId);
		if (depositsToRelease.isEmpty()) {
			log.info("경매 {} 환급 대상 없음", auctionId);
			return;
		}

		List<Long> memberIds = depositsToRelease.stream()
			.map(d -> d.getMember().getId())
			.toList();
		Map<Long, Wallet> walletMap = paymentSupport.findWalletsByMemberIdsForUpdate(memberIds);

		// 트랜잭션 이력 벌크 저장
		List<PaymentTransaction> transactions = new ArrayList<>();
		for (Deposit deposit : depositsToRelease) {
			Long memberId = deposit.getMember().getId();
			Wallet wallet = walletMap.get(memberId);

			if (wallet == null) {
				throw new CustomException(ErrorType.WALLET_NOT_FOUND);
			}

			PaymentTransaction transaction = releaseDeposit(deposit, wallet);
			if (transaction != null) {
				transactions.add(transaction);
			}
		}

		if (!transactions.isEmpty()) {
			transactionRepository.saveAll(transactions);
		}
		log.info("경매 {} 보증금 환급 완료: {}명", auctionId, depositsToRelease.size());
	}

	@Transactional
	public void releaseDeposit(Long auctionId, String memberPublicId) {
		PaymentMember member = paymentSupport.findMemberByPublicId(memberPublicId);
		releaseDeposit(auctionId, member, true);
	}

	@Transactional
	public boolean releaseDepositByMemberIdForRecovery(Long auctionId, Long memberId) {
		PaymentMember member = paymentSupport.findMemberById(memberId);
		return releaseDeposit(auctionId, member, false);
	}

	private boolean releaseDeposit(Long auctionId, PaymentMember member, boolean markSagaCompletion) {
		Deposit deposit = depositRepository.findByMemberIdAndAuctionId(member.getId(), auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.DEPOSIT_NOT_FOUND));

		if (markSagaCompletion && shouldSkipReleaseForConfirmedHold(auctionId, member.getId(), deposit)) {
			log.info("보증금 홀드가 이미 확정된 상태이므로 환급 건너뜀: auctionId={}, memberId={}", auctionId, member.getId());
			return false;
		}

		if (deposit.getStatus() != DepositStatus.HOLD) {
			log.info("보증금이 HOLD 상태가 아님 (상태: {}), 환급 건너뜀", deposit.getStatus());
			if (markSagaCompletion) {
				markDepositHoldSagaCompensated(auctionId, member.getId());
			}
			return false;
		}
		Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(member.getId());

		if (wallet == null) {
			throw new CustomException(ErrorType.WALLET_NOT_FOUND);
		}

		PaymentTransaction transaction = releaseDeposit(deposit, wallet);
		if (transaction != null) {
			transactionRepository.save(transaction);
		}
		if (markSagaCompletion) {
			markDepositHoldSagaCompensated(auctionId, member.getId());
		}

		log.info("단건 보증금 환급 완료: auctionId={}, memberId={}", auctionId, member.getId());
		return transaction != null;
	}

	private List<Deposit> findDepositsToRelease(Long auctionId, Long winnerId) {
		if (winnerId == null) {
			// 유찰인 경우: 모든 HOLD 상태 보증금 환급
			return depositRepository.findAllByAuctionIdAndStatusWithMember(auctionId, DepositStatus.HOLD);
		}
		// 낙찰자 제외
		return depositRepository.findAllByAuctionIdAndStatusAndMemberIdNotWithMember(auctionId, DepositStatus.HOLD,
			winnerId);
	}

	private PaymentTransaction releaseDeposit(Deposit deposit, Wallet wallet) {
		boolean releasedNow = deposit.release();
		if (!releasedNow) {
			return null;
		}

		// 2. Wallet holdingAmount 감소
		wallet.release(deposit.getAmount());

		log.info("보증금 환급: memberId={}, amount={}", deposit.getMember().getId(), deposit.getAmount());

		// 3. 이력 기록 반환
		return PaymentTransaction.builder()
			.member(deposit.getMember())
			.wallet(wallet)
			.transactionType(WalletTransactionType.DEPOSIT_RELEASE)
			.balanceDelta(0)
			.holdingDelta(-deposit.getAmount())
			.balanceAfter(wallet.getBalance())
			.referenceType(ReferenceType.DEPOSIT)
			.referenceId(deposit.getId())
			.build();
	}

	private void markDepositHoldSagaCompensated(Long auctionId, Long memberId) {
		String businessKey = String.format("%d:%d", auctionId, memberId);
		sagaTracker.markCheckpoint(PaymentSagaType.DEPOSIT_HOLD, businessKey,
			DepositHoldSagaStep.COMPENSATION_RELEASE_DONE);
		sagaTracker.markCompleted(PaymentSagaType.DEPOSIT_HOLD, businessKey, DepositHoldSagaStep.COMPLETED);
	}

	private boolean shouldSkipReleaseForConfirmedHold(Long auctionId, Long memberId, Deposit deposit) {
		if (deposit.getStatus() != DepositStatus.HOLD) {
			return false;
		}
		String businessKey = String.format("%d:%d", auctionId, memberId);
		return sagaExecutionRepository.findBySagaTypeAndBusinessKey(PaymentSagaType.DEPOSIT_HOLD, businessKey)
			.map(this::isConfirmedCompletedSaga)
			.orElse(false);
	}

	private boolean isConfirmedCompletedSaga(PaymentSagaExecution saga) {
		return saga.getStatus() == PaymentSagaExecutionStatus.COMPLETED
			&& DepositHoldSagaStep.CONFIRMED.name().equals(saga.getCurrentStep());
	}
}
