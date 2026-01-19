package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.ReferenceType;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType;
import com.bugzero.rarego.boundedContext.payment.in.dto.WalletTransactionResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;

@ExtendWith(MockitoExtension.class)
class PaymentGetWalletTransactionsUseCaseTest {
	@InjectMocks
	private PaymentGetWalletTransactionsUseCase useCase;

	@Mock
	private PaymentTransactionRepository paymentTransactionRepository;

	@Test
	@DisplayName("지갑 거래 내역을 조회하면 DTO로 변환된 페이지 결과를 반환한다")
	void getWalletTransactions_success() {
		// given
		Long memberId = 1L;
		int page = 0;
		int size = 10;
		WalletTransactionType type = WalletTransactionType.TOPUP_DONE;

		// 1. 실제 객체 생성 (Builder 사용)
		PaymentTransaction transaction = PaymentTransaction.builder()
			.member(mock(PaymentMember.class))
			.wallet(mock(Wallet.class))
			.transactionType(type)
			.balanceDelta(10000)
			.holdingDelta(0)
			.balanceAfter(50000)
			.referenceType(ReferenceType.PAYMENT)
			.referenceId(123L)
			.build();

		// 2. ID와 CreatedAt은 Setter가 없으므로 Reflection으로 주입
		ReflectionTestUtils.setField(transaction, "id", 100L);
		// BaseIdAndTime 내부 필드명이 "createdAt"이라고 가정
		ReflectionTestUtils.setField(transaction, "createdAt", LocalDateTime.now());

		Page<PaymentTransaction> entityPage = new PageImpl<>(List.of(transaction));

		// 3. Repository 동작 정의
		given(paymentTransactionRepository.findAllByMemberIdAndTransactionType(
			eq(memberId),
			eq(type),
			any(Pageable.class))
		).willReturn(entityPage);

		// when
		PagedResponseDto<WalletTransactionResponseDto> result =
			useCase.getWalletTransactions(memberId, page, size, type);

		// then
		// 결과 검증
		assertThat(result.data()).hasSize(1);

		WalletTransactionResponseDto dto = result.data().get(0);
		assertThat(dto.id()).isEqualTo(100L); // Reflection으로 넣은 ID 확인
		assertThat(dto.type()).isEqualTo(WalletTransactionType.TOPUP_DONE);
		assertThat(dto.typeName()).isEqualTo("예치금 충전");
		assertThat(dto.balance()).isEqualTo(50000);
	}

	@Test
	@DisplayName("거래 유형이 null이면(전체 조회) null을 Repository에 그대로 전달한다")
	void getWalletTransactions_withNullType() {
		// given
		Long memberId = 1L;
		Page<PaymentTransaction> emptyPage = Page.empty();

		given(paymentTransactionRepository.findAllByMemberIdAndTransactionType(
			any(), any(), any()))
			.willReturn(emptyPage);

		// when
		useCase.getWalletTransactions(memberId, 0, 10, null);

		// then
		verify(paymentTransactionRepository).findAllByMemberIdAndTransactionType(
			eq(memberId),
			eq(null), // null이 정확히 넘어갔는지 확인
			any(Pageable.class)
		);
	}
}