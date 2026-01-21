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
		// publicId → memberId 변환
		PaymentMember member = paymentSupport.findMemberByPublicId(request.memberPublicId());
		Long memberId = member.getId();

		// 1. 멱등성 체크 (memberId로 조회)
		return depositRepository.findByMemberIdAndAuctionId(memberId, request.auctionId())
			.map(DepositHoldResponseDto::from)
			.orElseGet(() -> executeHold(member, request));
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

		return DepositHoldResponseDto.from(deposit);
	}
}
