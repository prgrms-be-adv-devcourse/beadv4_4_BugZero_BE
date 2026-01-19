package com.bugzero.rarego.boundedContext.payment.app;

import java.time.LocalDateTime;

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
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentResponseDto;
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
public class PaymentAuctionFinalUseCase {
        private final AuctionOrderPort auctionOrderPort;
        private final DepositRepository depositRepository;
        private final PaymentTransactionRepository transactionRepository;
        private final SettlementRepository settlementRepository;
        private final PaymentSupport paymentSupport;

        @Transactional
        public AuctionFinalPaymentResponseDto finalPayment(Long memberId, Long auctionId,
                        AuctionFinalPaymentRequestDto request) {
                // TODO: 배송 로직 구현 시 request(배송 정보)를 사용하여 배송 정보 저장 필요
                // 1. 주문 조회 및 검증 (Port를 통해 Auction 모듈 접근)
                AuctionOrderDto order = findAndValidateOrder(auctionId, memberId);

                // 2. 보증금 조회
                Deposit deposit = findDeposit(memberId, auctionId);

                // 3. 금액 계산
                int finalPrice = order.finalPrice();
                int depositAmount = deposit.getAmount();
                int paymentAmount = finalPrice - depositAmount;

                // 4. 지갑 조회
                Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(memberId);
                PaymentMember buyer = paymentSupport.findMemberById(memberId);

                // 5. 보증금 사용 처리
                deposit.use();
                wallet.useDeposit(depositAmount);
                recordTransaction(buyer, wallet, WalletTransactionType.DEPOSIT_USED,
                                -depositAmount, -depositAmount, ReferenceType.DEPOSIT, deposit.getId());

                // 6. 잔금 결제 처리
                wallet.pay(paymentAmount);
                recordTransaction(buyer, wallet, WalletTransactionType.AUCTION_PAYMENT,
                                -paymentAmount, 0, ReferenceType.AUCTION_ORDER, order.orderId());

                // 7. 주문 완료 처리 (Port를 통해 Auction 모듈에 요청)
                auctionOrderPort.completeOrder(auctionId);

                // 8. 정산 정보 생성 (status = READY)
                PaymentMember seller = paymentSupport.findMemberById(order.sellerId());
                Settlement settlement = Settlement.create(auctionId, seller, finalPrice);
                settlementRepository.save(settlement);

                log.info("낙찰 결제 완료: auctionId={}, memberId={}, finalPrice={}, paid={}, settlementId={}",
                                auctionId, memberId, finalPrice, paymentAmount, settlement.getId());

                return AuctionFinalPaymentResponseDto.of(
                                order.orderId(),
                                auctionId,
                                buyer.getPublicId(),
                                finalPrice,
                                depositAmount,
                                wallet.getBalance(),
                                LocalDateTime.now());
        }

        private AuctionOrderDto findAndValidateOrder(Long auctionId, Long memberId) {
                AuctionOrderDto order = auctionOrderPort.findByAuctionId(auctionId)
                                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));

                if (!order.bidderId().equals(memberId)) {
                        throw new CustomException(ErrorType.NOT_AUCTION_WINNER);
                }

                if (!"PROCESSING".equals(order.status())) {
                        throw new CustomException(ErrorType.INVALID_ORDER_STATUS);
                }

                // 결제 기한 검증
                LocalDateTime deadline = order.createdAt().plusDays(3);
                if (LocalDateTime.now().isAfter(deadline)) {
                        throw new CustomException(ErrorType.PAYMENT_DEADLINE_EXCEEDED);
                }

                return order;
        }

        private Deposit findDeposit(Long memberId, Long auctionId) {
                return depositRepository.findByMemberIdAndAuctionId(memberId, auctionId)
                                .filter(d -> d.getStatus() == DepositStatus.HOLD)
                                .orElseThrow(() -> new CustomException(ErrorType.DEPOSIT_NOT_FOUND));
        }

        private void recordTransaction(PaymentMember member, Wallet wallet,
                        WalletTransactionType type, int balanceDelta, int holdingDelta,
                        ReferenceType refType, Long refId) {
                PaymentTransaction transaction = PaymentTransaction.builder()
                                .member(member)
                                .wallet(wallet)
                                .transactionType(type)
                                .balanceDelta(balanceDelta)
                                .holdingDelta(holdingDelta)
                                .balanceAfter(wallet.getBalance())
                                .referenceType(refType)
                                .referenceId(refId)
                                .build();
                transactionRepository.save(transaction);
        }
}
