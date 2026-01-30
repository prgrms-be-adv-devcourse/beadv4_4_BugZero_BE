package com.bugzero.rarego.boundedContext.payment.app;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.in.dto.RefundResponseDto;
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
public class PaymentRefundUseCase {
	private final AuctionOrderPort auctionOrderPort;
	private final SettlementRepository settlementRepository;
	private final PaymentTransactionRepository transactionRepository;
	private final PaymentSupport paymentSupport;

	@Transactional
	public RefundResponseDto processRefund(Long auctionId) {
		// 1. 주문 조회 및 검증 (SUCCESS 상태만)
		AuctionOrderDto order = auctionOrderPort.refundOrderWithLock(auctionId);

		// 2. 정산 상태 확인 및 락 (READY 상태만 환불 가능)
		Settlement settlement = findAndValidateSettlement(auctionId);

		// 3. 구매자 지갑 잔액 증가 (+finalPrice)
		Wallet buyerWallet = paymentSupport.findWalletByMemberIdForUpdate(order.bidderId());
		PaymentMember buyer = paymentSupport.findMemberById(order.bidderId());
		buyerWallet.addBalance(order.finalPrice());

		// 4. 환불 트랜잭션 생성
		PaymentTransaction transaction = recordRefundTransaction(buyer, buyerWallet, order);

		// 5. Settlement 상태 변경 (READY -> FAILED)
		settlement.cancel();

		log.info("환불 처리 완료: auctionId={}, buyerId={}, refundAmount={}",
			auctionId, order.bidderId(), order.finalPrice());

		return new RefundResponseDto(
			transaction.getId(),
			auctionId,
			order.finalPrice(),
			buyer.getPublicId(),
			buyerWallet.getBalance(),
			transaction.getCreatedAt());
	}

	private Settlement findAndValidateSettlement(Long auctionId) {
		Optional<Settlement> settlementOpt = settlementRepository.findByAuctionIdForUpdate(auctionId);
		if (settlementOpt.isEmpty()) {
			throw new CustomException(ErrorType.SETTLEMENT_NOT_FOUND);
		}

		Settlement settlement = settlementOpt.get();

		// 정산 완료(DONE) 후에는 환불 불가
		if (settlement.getStatus() != SettlementStatus.READY) {
			throw new CustomException(ErrorType.SETTLEMENT_ALREADY_COMPLETED);
		}

		return settlement;
	}

	private PaymentTransaction recordRefundTransaction(PaymentMember buyer, Wallet wallet, AuctionOrderDto order) {
		PaymentTransaction transaction = PaymentTransaction.builder()
			.member(buyer)
			.wallet(wallet)
			.transactionType(WalletTransactionType.REFUND_DONE)
			.balanceDelta(order.finalPrice())
			.holdingDelta(0)
			.balanceAfter(wallet.getBalance())
			.referenceType(ReferenceType.AUCTION_ORDER)
			.referenceId(order.orderId())
			.build();
		return transactionRepository.save(transaction);
	}
}
