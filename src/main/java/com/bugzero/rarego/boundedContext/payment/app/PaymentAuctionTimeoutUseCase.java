package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.out.DepositRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.auction.port.AuctionOrderPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentAuctionTimeoutUseCase {
    private final AuctionOrderPort auctionOrderPort;
    private final DepositRepository depositRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final PaymentSupport paymentSupport;

    @Transactional
    public void processTimeout(Long auctionId) {
        // 1. 주문 조회 및 검증 (PROCESSING 상태만)
        AuctionOrderDto order = findAndValidateOrder(auctionId);

        // 2. 보증금 조회 (HOLD 상태만)
        Deposit deposit = findDeposit(order.bidderId(), auctionId);

        // 3. 보증금 몰수 처리
        deposit.forfeit();
        Wallet buyerWallet = paymentSupport.findWalletByMemberIdForUpdate(order.bidderId());
        PaymentMember buyer = paymentSupport.findMemberById(order.bidderId());
        buyerWallet.forfeitDeposit(deposit.getAmount());
        recordTransaction(buyer, buyerWallet,
            -deposit.getAmount(), -deposit.getAmount(), deposit.getId());

        // 4. 주문 실패 처리
        auctionOrderPort.failOrder(auctionId);

        // 5. 판매자 정산 생성 (보증금 기반)
        PaymentMember seller = paymentSupport.findMemberById(order.sellerId());
        Settlement settlement = Settlement.createFromForfeit(auctionId, seller, deposit.getAmount());
        settlementRepository.save(settlement);

        log.info("타임아웃 처리 완료: auctionId={}, bidderId={}, depositAmount={}, settlementId={}",
                auctionId, order.bidderId(), deposit.getAmount(), settlement.getId());
    }

    private AuctionOrderDto findAndValidateOrder(Long auctionId) {
        AuctionOrderDto order = auctionOrderPort.findByAuctionIdForUpdate(auctionId)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));

        if (!"PROCESSING".equals(order.status())) {
            throw new CustomException(ErrorType.INVALID_ORDER_STATUS);
        }

        return order;
    }

    private Deposit findDeposit(Long memberId, Long auctionId) {
        return depositRepository.findByMemberIdAndAuctionId(memberId, auctionId)
                .filter(d -> d.getStatus() == DepositStatus.HOLD)
                .orElseThrow(() -> new CustomException(ErrorType.DEPOSIT_NOT_FOUND));
    }

    private void recordTransaction(PaymentMember member, Wallet wallet,
        int balanceDelta, int holdingDelta,
        Long refId) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .member(member)
                .wallet(wallet)
                .transactionType(WalletTransactionType.DEPOSIT_FORFEITED)
                .balanceDelta(balanceDelta)
                .holdingDelta(holdingDelta)
                .balanceAfter(wallet.getBalance())
                .referenceType(ReferenceType.DEPOSIT)
                .referenceId(refId)
                .build();
        transactionRepository.save(transaction);
    }
}

