package com.bugzero.rarego.boundedContext.payment.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.out.DepositRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;

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

    @Transactional
    public void releaseDeposits(Long auctionId, Long winnerId) {
        List<Deposit> depositsToRelease = findDepositsToRelease(auctionId, winnerId);

        for (Deposit deposit : depositsToRelease) {
            releaseDeposit(deposit);
        }

        log.info("경매 {} 보증금 환급 완료: {}명", auctionId, depositsToRelease.size());
    }

    private List<Deposit> findDepositsToRelease(Long auctionId, Long winnerId) {
        if (winnerId == null) {
            // 유찰인 경우: 모든 HOLD 상태 보증금 환급
            return depositRepository.findAllByAuctionIdAndStatus(auctionId, DepositStatus.HOLD);
        }
        // 낙찰자 제외
        return depositRepository.findAllByAuctionIdAndStatusAndMemberIdNot(
			auctionId, DepositStatus.HOLD, winnerId);
    }

    private void releaseDeposit(Deposit deposit) {
        Long memberId = deposit.getMember().getId();

        // 1. Deposit 상태 변경
        deposit.release();

        // 2. Wallet holdingAmount 감소
        Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(memberId);
        wallet.release(deposit.getAmount());

        // 3. 이력 기록
        PaymentTransaction transaction = PaymentTransaction.builder()
                .member(deposit.getMember())
                .wallet(wallet)
                .transactionType(WalletTransactionType.DEPOSIT_RELEASE)
                .balanceDelta(0)
                .holdingDelta(-deposit.getAmount())
                .balanceAfter(wallet.getBalance())
                .referenceType(ReferenceType.DEPOSIT)
                .referenceId(deposit.getId())
                .build();
        transactionRepository.save(transaction);

        log.info("보증금 환급: memberId={}, amount={}", memberId, deposit.getAmount());
    }
}
