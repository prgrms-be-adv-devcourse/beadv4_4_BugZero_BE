package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.DepositRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

@ExtendWith(MockitoExtension.class)
class PaymentHoldDepositUseCaseTest {

	private static final String MEMBER_PUBLIC_ID = "member-uuid-123";
	private static final Long MEMBER_ID = 4L;
	private static final Long AUCTION_ID = 3L;

	@InjectMocks
	private PaymentHoldDepositUseCase paymentHoldDepositUseCase;

	@Mock
	private DepositRepository depositRepository;

	@Mock
	private PaymentTransactionRepository transactionRepository;

	@Mock
	private PaymentSupport paymentSupport;

	@Test
	@DisplayName("보증금 홀딩 성공")
	void holdDeposit_Success() {
		// given
		DepositHoldRequestDto request = new DepositHoldRequestDto(20000, MEMBER_PUBLIC_ID, AUCTION_ID);
		PaymentMember member = mock(PaymentMember.class);
		Wallet wallet = Wallet.builder().balance(100000).holdingAmount(0).build();

		when(member.getId()).thenReturn(MEMBER_ID);
		given(paymentSupport.findMemberByPublicId(MEMBER_PUBLIC_ID)).willReturn(member);
		when(depositRepository.findByMemberIdAndAuctionId(MEMBER_ID, AUCTION_ID)).thenReturn(Optional.empty());
		given(paymentSupport.findWalletByMemberIdForUpdate(MEMBER_ID)).willReturn(wallet);

		// when
		DepositHoldResponseDto response = paymentHoldDepositUseCase.holdDeposit(request);

		// then
		assertThat(wallet.getHoldingAmount()).isEqualTo(20000);
		assertThat(response.amount()).isEqualTo(20000);
		assertThat(response.auctionId()).isEqualTo(AUCTION_ID);
		assertThat(response.status()).isEqualTo("HOLD");
		verify(depositRepository, times(1)).save(any(Deposit.class));
		verify(transactionRepository, times(1)).save(any(PaymentTransaction.class));
	}

	@Test
	@DisplayName("보증금 홀딩 실패 - 잔액 부족")
	void holdDeposit_Fail_InsufficientBalance() {
		// given
		DepositHoldRequestDto request = new DepositHoldRequestDto(20000, MEMBER_PUBLIC_ID, AUCTION_ID);
		PaymentMember member = mock(PaymentMember.class);
		Wallet wallet = Wallet.builder().balance(10000).holdingAmount(0).build();

		when(member.getId()).thenReturn(MEMBER_ID);
		given(paymentSupport.findMemberByPublicId(MEMBER_PUBLIC_ID)).willReturn(member);
		when(depositRepository.findByMemberIdAndAuctionId(MEMBER_ID, AUCTION_ID)).thenReturn(Optional.empty());
		given(paymentSupport.findWalletByMemberIdForUpdate(MEMBER_ID)).willReturn(wallet);

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
		DepositHoldRequestDto request = new DepositHoldRequestDto(20000, MEMBER_PUBLIC_ID, AUCTION_ID);
		PaymentMember member = mock(PaymentMember.class);
		Deposit existingDeposit = mock(Deposit.class);

		when(member.getId()).thenReturn(MEMBER_ID);
		when(existingDeposit.getId()).thenReturn(100L);
		when(existingDeposit.getAuctionId()).thenReturn(AUCTION_ID);
		when(existingDeposit.getAmount()).thenReturn(20000);
		when(existingDeposit.getStatus()).thenReturn(DepositStatus.HOLD);

		given(paymentSupport.findMemberByPublicId(MEMBER_PUBLIC_ID)).willReturn(member);
		when(depositRepository.findByMemberIdAndAuctionId(MEMBER_ID, AUCTION_ID))
				.thenReturn(Optional.of(existingDeposit));

		// when
		DepositHoldResponseDto response = paymentHoldDepositUseCase.holdDeposit(request);

		// then
		assertThat(response.depositId()).isEqualTo(100L);
		assertThat(response.auctionId()).isEqualTo(AUCTION_ID);
		assertThat(response.amount()).isEqualTo(20000);
		assertThat(response.status()).isEqualTo("HOLD");
		verify(depositRepository, never()).save(any());
		verify(transactionRepository, never()).save(any());
	}
}
