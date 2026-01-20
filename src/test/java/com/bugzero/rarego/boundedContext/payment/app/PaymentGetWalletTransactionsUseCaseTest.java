package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
	@DisplayName("지갑 거래 내역을 조회하면 날짜 조건이 시간으로 변환되어 Repository를 호출하고 결과를 반환한다")
	void getWalletTransactions_success() {
		// given
		Long memberId = 1L;
		int page = 0;
		int size = 10;
		WalletTransactionType type = WalletTransactionType.TOPUP_DONE;

		// ✅ [Input] 날짜 조건 (LocalDate)
		LocalDate fromDate = LocalDate.of(2024, 1, 1);
		LocalDate toDate = LocalDate.of(2024, 1, 31);

		// ✅ [Expected] Service 내부에서 변환될 시간값 예상
		LocalDateTime expectedFrom = fromDate.atStartOfDay();      // 2024-01-01 00:00:00
		LocalDateTime expectedTo = toDate.atTime(LocalTime.MAX);   // 2024-01-31 23:59:59...

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

		ReflectionTestUtils.setField(transaction, "id", 100L);
		ReflectionTestUtils.setField(transaction, "createdAt", LocalDateTime.now());

		Page<PaymentTransaction> entityPage = new PageImpl<>(List.of(transaction));

		// 2. Repository 동작 정의 (변경된 메서드명과 파라미터 매칭 확인)
		given(paymentTransactionRepository.findAllByMemberIdAndTransactionType(
			eq(memberId),
			eq(type),
			eq(expectedFrom), // ★ 변환된 LocalDateTime이 넘어오는지 검증
			eq(expectedTo),   // ★ 변환된 LocalDateTime이 넘어오는지 검증
			any(Pageable.class))
		).willReturn(entityPage);

		// when
		// ✅ LocalDate 파라미터 전달
		PagedResponseDto<WalletTransactionResponseDto> result =
			useCase.getWalletTransactions(memberId, page, size, type, fromDate, toDate);

		// then
		// 결과 검증
		assertThat(result.data()).hasSize(1);

		WalletTransactionResponseDto dto = result.data().get(0);
		assertThat(dto.id()).isEqualTo(100L);
		assertThat(dto.type()).isEqualTo(WalletTransactionType.TOPUP_DONE);
		assertThat(dto.typeName()).isEqualTo("예치금 충전");
		assertThat(dto.balance()).isEqualTo(50000);

		// 호출 검증
		verify(paymentTransactionRepository).findAllByMemberIdAndTransactionType(
			eq(memberId), eq(type), eq(expectedFrom), eq(expectedTo), any(Pageable.class)
		);
	}

	@Test
	@DisplayName("검색 조건(Type, Date)이 null이면 Repository에 null을 그대로 전달한다")
	void getWalletTransactions_withNullParams() {
		// given
		Long memberId = 1L;
		Page<PaymentTransaction> emptyPage = Page.empty();

		given(paymentTransactionRepository.findAllByMemberIdAndTransactionType(
			any(), any(), any(), any(), any())) // 인자 개수 5개로 변경됨
			.willReturn(emptyPage);

		// when
		// ✅ Type, From, To 모두 null 전달
		useCase.getWalletTransactions(memberId, 0, 10, null, null, null);

		// then
		verify(paymentTransactionRepository).findAllByMemberIdAndTransactionType(
			eq(memberId),
			eq(null), // Type
			eq(null), // From
			eq(null), // To
			any(Pageable.class)
		);
	}
}