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
import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.in.dto.SettlementResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;

@ExtendWith(MockitoExtension.class)
class PaymentGetSettlementsUseCaseTest {

	@InjectMocks
	private PaymentGetSettlementsUseCase useCase;

	@Mock
	private SettlementRepository settlementRepository;

	@Test
	@DisplayName("정산 내역 조회 시 LocalDate를 시간 범위(Start~End)로 변환하여 Repository를 호출한다")
	void getSettlements_success() {
		// given
		Long memberId = 1L;
		Long sellerId = 1L;
		int page = 0;
		int size = 10;
		SettlementStatus status = SettlementStatus.READY;

		// ✅ [변경] 입력값: LocalDate (날짜만)
		LocalDate fromDate = LocalDate.now().minusDays(7);
		LocalDate toDate = LocalDate.now();

		// ✅ [변경] 검증값: Service 내부에서 변환될 LocalDateTime 예상값
		LocalDateTime expectedFrom = fromDate.atStartOfDay();      // 00:00:00
		LocalDateTime expectedTo = toDate.atTime(LocalTime.MAX);   // 23:59:59.999...

		// 1. 연관 관계 Mocking
		PaymentMember mockSeller = mock(PaymentMember.class);
		given(mockSeller.getId()).willReturn(sellerId);

		// 2. Settlement 객체 생성
		Settlement settlement = Settlement.builder()
			.auctionId(100L)
			.seller(mockSeller)
			.salesAmount(10000)
			.feeAmount(1000)
			.settlementAmount(9000)
			.status(status)
			.build();

		ReflectionTestUtils.setField(settlement, "id", 50L);
		ReflectionTestUtils.setField(settlement, "createdAt", LocalDateTime.now());

		Page<Settlement> settlementPage = new PageImpl<>(List.of(settlement));

		// 3. Repository Mocking (변환된 LocalDateTime으로 호출될 것을 정의)
		given(settlementRepository.findAllBySellerIdAndStatus(
			eq(memberId),
			eq(status),
			eq(expectedFrom), // ★ 변환된 시간 검증
			eq(expectedTo),   // ★ 변환된 시간 검증
			any(Pageable.class))
		).willReturn(settlementPage);

		// when
		// ✅ [변경] LocalDate 전달
		PagedResponseDto<SettlementResponseDto> result =
			useCase.getSettlements(memberId, page, size, status, fromDate, toDate);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).id()).isEqualTo(50L);

		// 4. Repository 호출 파라미터 검증
		verify(settlementRepository).findAllBySellerIdAndStatus(
			eq(memberId),
			eq(status),
			eq(expectedFrom), // ★ 00:00:00 확인
			eq(expectedTo),   // ★ 23:59:59 확인
			any(Pageable.class)
		);
	}

	@Test
	@DisplayName("날짜 조건이 null이면 Repository에도 null(시간 범위 없음)이 전달된다")
	void getSettlements_withNullParams() {
		// given
		Long memberId = 1L;
		Page<Settlement> emptyPage = Page.empty();

		// null -> null 변환 확인
		given(settlementRepository.findAllBySellerIdAndStatus(
			eq(memberId),
			eq(null),
			eq(null), // fromDateTime
			eq(null), // toDateTime
			any(Pageable.class))
		).willReturn(emptyPage);

		// when
		// ✅ LocalDate 파라미터 자리에 null 전달
		PagedResponseDto<SettlementResponseDto> result =
			useCase.getSettlements(memberId, 0, 10, null, null, null);

		// then
		assertThat(result.data()).isEmpty();

		verify(settlementRepository).findAllBySellerIdAndStatus(
			eq(memberId),
			eq(null),
			eq(null),
			eq(null),
			any(Pageable.class)
		);
	}
}