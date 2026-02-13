package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Deposit;
import com.bugzero.rarego.domain.DepositStatus;
import com.bugzero.rarego.domain.PaymentMember;
import com.bugzero.rarego.domain.PaymentTransaction;
import com.bugzero.rarego.domain.ReferenceType;
import com.bugzero.rarego.domain.Wallet;
import com.bugzero.rarego.domain.WalletTransactionType;
import com.bugzero.rarego.out.DepositRepository;
import com.bugzero.rarego.out.PaymentTransactionRepository;
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
			.map(deposit -> {
				if (deposit.getStatus() == DepositStatus.RELEASED) {
					// 이미 환급된 경우: 다시 돈을 묶음 (재입찰)
					return executeReHold(deposit, member, request.amount());
				}
				// 이미 HOLD 상태이거나 다른 처리 중인 경우: 기존 정보 반환
				return new DepositHoldResponseDto(
					deposit.getId(),
					deposit.getAuctionId(),
					deposit.getAmount(),
					deposit.getStatus().name(),
					deposit.getCreatedAt());
			})
			.orElseGet(() -> executeHold(member, request));
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

		return new DepositHoldResponseDto(
			deposit.getId(),
			deposit.getAuctionId(),
			deposit.getAmount(),
			deposit.getStatus().name(),
			deposit.getCreatedAt());
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

		return new DepositHoldResponseDto(
			deposit.getId(),
			deposit.getAuctionId(),
			deposit.getAmount(),
			deposit.getStatus().name(),
			deposit.getCreatedAt()
		);
	}
}
