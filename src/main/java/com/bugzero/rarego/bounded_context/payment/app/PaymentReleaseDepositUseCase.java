package com.bugzero.rarego.bounded_context.payment.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.bounded_context.payment.domain.Deposit;
import com.bugzero.rarego.bounded_context.payment.domain.DepositStatus;
import com.bugzero.rarego.bounded_context.payment.domain.PaymentTransaction;
import com.bugzero.rarego.bounded_context.payment.domain.ReferenceType;
import com.bugzero.rarego.bounded_context.payment.domain.Wallet;
import com.bugzero.rarego.bounded_context.payment.domain.WalletTransactionType;
import com.bugzero.rarego.bounded_context.payment.out.DepositRepository;
import com.bugzero.rarego.bounded_context.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

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
            transactions.add(transaction);
        }

        transactionRepository.saveAll(transactions);
        log.info("경매 {} 보증금 환급 완료: {}명", auctionId, depositsToRelease.size());
    }

    private List<Deposit> findDepositsToRelease(Long auctionId, Long winnerId) {
        if (winnerId == null) {
            // 유찰인 경우: 모든 HOLD 상태 보증금 환급
            return depositRepository.findAllByAuctionIdAndStatusWithMember(auctionId, DepositStatus.HOLD);
        }
        // 낙찰자 제외
        return depositRepository.findAllByAuctionIdAndStatusAndMemberIdNotWithMember(auctionId, DepositStatus.HOLD, winnerId);
    }

    private PaymentTransaction releaseDeposit(Deposit deposit, Wallet wallet) {
        // 1. Deposit 상태 변경
        deposit.release();

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
}
