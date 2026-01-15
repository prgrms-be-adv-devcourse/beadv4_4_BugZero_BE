package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.out.DepositRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentHoldDepositUseCase {
	private final DepositRepository depositRepository;
	private final PaymentTransactionRepository transactionRepository;
	private final PaymentSupport paymentSupport;

	@Transactional
	public DepositHoldResponseDto holdDeposit(DepositHoldRequestDto request) {
		// 1. 멱등성 체크
		return depositRepository.findByMemberIdAndAuctionId(request.memberId(), request.auctionId())
			.map(DepositHoldResponseDto::from)
			.orElseGet(() -> executeHold(request));
	}

	private DepositHoldResponseDto executeHold(DepositHoldRequestDto request) {
		PaymentMember member = paymentSupport.findMemberById(request.memberId());

		Wallet wallet = paymentSupport.findWalletByMemberIdForUpdate(request.memberId());

		// 2. 잔액 검증 & Wallet 업데이트
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

		return DepositHoldResponseDto.from(deposit);
	}
}
