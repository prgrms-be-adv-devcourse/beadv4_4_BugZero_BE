package com.bugzero.rarego.bounded_context.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
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

import com.bugzero.rarego.bounded_context.payment.domain.PaymentMember;
import com.bugzero.rarego.bounded_context.payment.domain.PaymentTransaction;
import com.bugzero.rarego.bounded_context.payment.domain.ReferenceType;
import com.bugzero.rarego.bounded_context.payment.domain.Wallet;
import com.bugzero.rarego.bounded_context.payment.domain.WalletTransactionType;
import com.bugzero.rarego.bounded_context.payment.in.dto.WalletTransactionResponseDto;
import com.bugzero.rarego.bounded_context.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;

@ExtendWith(MockitoExtension.class)
class PaymentGetWalletTransactionsUseCaseTest {
	@InjectMocks
	private PaymentGetWalletTransactionsUseCase useCase;

	@Mock
	private PaymentTransactionRepository paymentTransactionRepository;

	@Mock
	private PaymentSupport paymentSupport;

	@Test
	@DisplayName("지갑 거래 내역을 조회하면 날짜 조건이 시간으로 변환되어 Repository를 호출하고 결과를 반환한다")
	void getWalletTransactions_success() {
		// given
		String memberPublicId = "uuid-member-1"; // [변경] 입력값 String
		Long memberId = 1L; // [변경] 내부 ID
		int page = 0;
		int size = 10;
		WalletTransactionType type = WalletTransactionType.TOPUP_DONE;

		LocalDate fromDate = LocalDate.of(2024, 1, 1);
		LocalDate toDate = LocalDate.of(2024, 1, 31);

		LocalDateTime expectedFrom = fromDate.atStartOfDay();
		LocalDateTime expectedTo = toDate.plusDays(1).atStartOfDay();

		// [추가] Member 조회 Mocking
		PaymentMember mockMember = mock(PaymentMember.class);
		given(mockMember.getId()).willReturn(memberId);
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(mockMember);

		// 1. Transaction 객체 생성
		PaymentTransaction transaction = PaymentTransaction.builder()
			.member(mockMember)
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

		// 2. Repository 동작 정의 (조회된 memberId가 전달되는지 확인)
		given(paymentTransactionRepository.searchPaymentTransactions(
			eq(memberId), // ★ PublicId가 아닌 조회된 Long ID가 들어가야 함
			eq(type),
			eq(expectedFrom),
			eq(expectedTo),
			any(Pageable.class))
		).willReturn(entityPage);

		// when
		PagedResponseDto<WalletTransactionResponseDto> result =
			useCase.getWalletTransactions(memberPublicId, page, size, type, fromDate, toDate);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).id()).isEqualTo(100L);

		// 호출 검증
		verify(paymentSupport).findMemberByPublicId(memberPublicId); // ID 조회 호출 확인
		verify(paymentTransactionRepository).searchPaymentTransactions(
			eq(memberId), eq(type), eq(expectedFrom), eq(expectedTo), any(Pageable.class)
		);
	}

	@Test
	@DisplayName("검색 조건(Type, Date)이 null이면 Repository에 null을 그대로 전달한다")
	void getWalletTransactions_withNullParams() {
		// given
		String memberPublicId = "uuid-member-1";
		Long memberId = 1L;
		Page<PaymentTransaction> emptyPage = Page.empty();

		// [추가] Member 조회 Mocking
		PaymentMember mockMember = mock(PaymentMember.class);
		given(mockMember.getId()).willReturn(memberId);
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(mockMember);

		given(paymentTransactionRepository.searchPaymentTransactions(
			any(), any(), any(), any(), any()))
			.willReturn(emptyPage);

		// when
		useCase.getWalletTransactions(memberPublicId, 0, 10, null, null, null);

		// then
		verify(paymentTransactionRepository).searchPaymentTransactions(
			eq(memberId), // ★ 변환된 ID 확인
			eq(null),
			eq(null),
			eq(null),
			any(Pageable.class)
		);
	}
}