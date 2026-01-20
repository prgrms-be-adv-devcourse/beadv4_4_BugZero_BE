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

	@Mock
	private PaymentSupport paymentSupport; // [추가]

	@Test
	@DisplayName("정산 내역 조회 시 날짜 조건이 시간으로 변환되어 Repository를 호출한다")
	void getSettlements_success() {
		// given
		String memberPublicId = "uuid-member-1"; // [변경] 입력값 String
		Long memberId = 1L; // [변경] 내부 ID
		Long sellerId = 1L;
		int page = 0;
		int size = 10;
		SettlementStatus status = SettlementStatus.READY;

		LocalDate fromDate = LocalDate.now().minusDays(7);
		LocalDate toDate = LocalDate.now();

		LocalDateTime expectedFrom = fromDate.atStartOfDay();
		LocalDateTime expectedTo = toDate.atTime(LocalTime.MAX);

		// [추가] Member 조회 Mocking
		PaymentMember mockSeller = mock(PaymentMember.class);
		given(mockSeller.getId()).willReturn(sellerId);

		// paymentSupport가 memberPublicId로 조회 시 mockSeller를 반환
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(mockSeller);

		// 1. Settlement 객체 생성
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

		// 2. Repository Mocking (조회된 Long ID가 전달되는지 확인)
		given(settlementRepository.searchSettlements(
			eq(memberId), // ★ PublicId가 아닌 조회된 Long ID 검증
			eq(status),
			eq(expectedFrom),
			eq(expectedTo),
			any(Pageable.class))
		).willReturn(settlementPage);

		// when
		// ✅ String PublicId 전달
		PagedResponseDto<SettlementResponseDto> result =
			useCase.getSettlements(memberPublicId, page, size, status, fromDate, toDate);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).id()).isEqualTo(50L);

		// 호출 검증
		verify(paymentSupport).findMemberByPublicId(memberPublicId); // ID 조회 호출 확인
		verify(settlementRepository).searchSettlements(
			eq(memberId),
			eq(status),
			eq(expectedFrom),
			eq(expectedTo),
			any(Pageable.class)
		);
	}

	@Test
	@DisplayName("날짜 조건이 null이면 Repository에도 null(시간 범위 없음)이 전달된다")
	void getSettlements_withNullParams() {
		// given
		String memberPublicId = "uuid-member-1";
		Long memberId = 1L;
		Page<Settlement> emptyPage = Page.empty();

		// [추가] Member 조회 Mocking
		PaymentMember mockMember = mock(PaymentMember.class);
		given(mockMember.getId()).willReturn(memberId);
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(mockMember);

		// null -> null 변환 확인
		given(settlementRepository.searchSettlements(
			eq(memberId), // ★ 변환된 ID 확인
			eq(null),
			eq(null),
			eq(null),
			any(Pageable.class))
		).willReturn(emptyPage);

		// when
		PagedResponseDto<SettlementResponseDto> result = useCase.getSettlements(memberPublicId, 0, 10, null, null,
			null);

		// then
		assertThat(result.data()).isEmpty();

		verify(settlementRepository).searchSettlements(
			eq(memberId),
			eq(null),
			eq(null),
			eq(null),
			any(Pageable.class)
		);
	}
}