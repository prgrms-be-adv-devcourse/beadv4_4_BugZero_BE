package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.DepositRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

@ExtendWith(MockitoExtension.class)
class PaymentHoldDepositUseCaseTest {

	@InjectMocks
	private PaymentHoldDepositUseCase paymentHoldDepositUseCase;

	@Mock
	private DepositRepository depositRepository;

	@Mock
	private WalletRepository walletRepository;

	@Mock
	private PaymentMemberRepository memberRepository;

	@Mock
	private PaymentTransactionRepository transactionRepository;

	@Test
	@DisplayName("보증금 홀딩 성공")
	void holdDeposit_Success() {
		// given
		DepositHoldRequestDto request = new DepositHoldRequestDto(20000, 4L, 3L);
		PaymentMember member = mock(PaymentMember.class);
		Wallet wallet = Wallet.builder().balance(100000).holdingAmount(0).build();

		when(depositRepository.findByMemberIdAndAuctionId(4L, 3L)).thenReturn(Optional.empty());
		when(memberRepository.findById(4L)).thenReturn(Optional.of(member));
		when(walletRepository.findByMemberId(4L)).thenReturn(Optional.of(wallet));

		// when
		DepositHoldResponseDto response = paymentHoldDepositUseCase.holdDeposit(request);

		// then
		assertThat(wallet.getHoldingAmount()).isEqualTo(20000);
		assertThat(response.amount()).isEqualTo(20000);
		assertThat(response.auctionId()).isEqualTo(3L);
		assertThat(response.status()).isEqualTo("HOLD");
		verify(depositRepository, times(1)).save(any(Deposit.class));
		verify(transactionRepository, times(1)).save(any(PaymentTransaction.class));
	}

	@Test
	@DisplayName("보증금 홀딩 실패 - 잔액 부족")
	void holdDeposit_Fail_InsufficientBalance() {
		// given
		DepositHoldRequestDto request = new DepositHoldRequestDto(20000, 4L, 3L);
		Wallet wallet = Wallet.builder().balance(10000).holdingAmount(0).build();

		when(depositRepository.findByMemberIdAndAuctionId(4L, 3L)).thenReturn(Optional.empty());
		when(memberRepository.findById(4L)).thenReturn(Optional.of(mock(PaymentMember.class)));
		when(walletRepository.findByMemberId(4L)).thenReturn(Optional.of(wallet));

		// when & then
		assertThatThrownBy(() -> paymentHoldDepositUseCase.holdDeposit(request))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.INSUFFICIENT_BALANCE);
	}

	@Test
	@DisplayName("보증금 홀딩 멱등성 검증 - 이미 홀딩된 경우 기존 Deposit 반환")
	void holdDeposit_Idempotency() {
		// given
		DepositHoldRequestDto request = new DepositHoldRequestDto(20000, 4L, 3L);
		Deposit existingDeposit = mock(Deposit.class);
		when(existingDeposit.getId()).thenReturn(100L);
		when(existingDeposit.getAuctionId()).thenReturn(3L);
		when(existingDeposit.getAmount()).thenReturn(20000);
		when(existingDeposit.getStatus()).thenReturn(DepositStatus.HOLD);

		when(depositRepository.findByMemberIdAndAuctionId(4L, 3L))
			.thenReturn(Optional.of(existingDeposit));

		// when
		DepositHoldResponseDto response = paymentHoldDepositUseCase.holdDeposit(request);

		// then
		assertThat(response.depositId()).isEqualTo(100L);
		assertThat(response.status()).isEqualTo("HOLD");
		verify(walletRepository, never()).findByMemberId(anyLong());
		verify(depositRepository, never()).save(any());
		verify(transactionRepository, never()).save(any());
	}
}
