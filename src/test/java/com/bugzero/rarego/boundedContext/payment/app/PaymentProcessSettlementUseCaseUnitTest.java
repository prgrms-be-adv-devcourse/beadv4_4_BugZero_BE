package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;

@ExtendWith(MockitoExtension.class)
class PaymentProcessSettlementUseCaseUnitTest {

	@InjectMocks
	private PaymentProcessSettlementUseCase useCase;

	@Mock
	private PaymentSettlementProcessor paymentSettlementProcessor;

	@Mock
	private SettlementRepository settlementRepository;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(useCase, "settlementHoldDays", 0);
	}

	@Test
	@DisplayName("정상 흐름: 2건 모두 성공 시 성공 카운트 반환 및 개별 처리 호출 검증")
	void success_all() {
		// given
		Settlement s1 = createSettlement(1L);
		Settlement s2 = createSettlement(2L);
		List<Settlement> list = List.of(s1, s2);

		given(settlementRepository.findSettlementsForBatch(eq(SettlementStatus.READY), any(), any(Pageable.class)))
			.willReturn(list);

		// [수정] process 메서드가 void를 반환하므로 doNothing() 혹은 기본 설정 사용
		doNothing().when(paymentSettlementProcessor).process(anyLong());

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(2);
		verify(paymentSettlementProcessor, times(2)).process(anyLong());
		verify(paymentSettlementProcessor, never()).fail(anyLong()); // 성공 시 fail 호출 안됨
	}

	@Test
	@DisplayName("부분 성공: 1건 성공, 1건 실패 시에도 중단되지 않고 성공 카운트 집계 및 fail() 호출")
	void partial_success() {
		// given
		Settlement successItem = createSettlement(1L);
		Settlement failItem = createSettlement(2L);

		given(settlementRepository.findSettlementsForBatch(eq(SettlementStatus.READY), any(), any(Pageable.class)))
			.willReturn(List.of(successItem, failItem));

		doNothing().when(paymentSettlementProcessor).process(1L);

		// 2번은 예외 발생
		doThrow(new RuntimeException("DB Lock Error"))
			.when(paymentSettlementProcessor).process(2L);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(1);
		
		verify(paymentSettlementProcessor).process(1L);
		verify(paymentSettlementProcessor).process(2L);
		verify(paymentSettlementProcessor).fail(2L);
	}

	@Test
	@DisplayName("치명적 실패: fail() 처리 도중 예외가 발생해도 전체 루프가 중단되지 않아야 함")
	void critical_failure_in_fail_marking() {
		// given
		Settlement failItem1 = createSettlement(1L);
		Settlement failItem2 = createSettlement(2L);

		given(settlementRepository.findSettlementsForBatch(any(), any(), any()))
			.willReturn(List.of(failItem1, failItem2));

		// 두 건 모두 process 실패
		doThrow(new RuntimeException("Process Error")).when(paymentSettlementProcessor).process(anyLong());

		// 첫 번째 건의 fail() 마킹마저 예외 발생 (가장 위험한 상황 가정)
		doThrow(new RuntimeException("Critical Marking Error")).when(paymentSettlementProcessor).fail(1L);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);
		// fail(1L)에서 예외가 났지만, 루프가 돌아 fail(2L)까지 시도는 했어야 함
		verify(paymentSettlementProcessor).fail(1L);
		verify(paymentSettlementProcessor).fail(2L);
	}

	@Test
	@DisplayName("데이터 없음: 0 반환 및 로직 스킵")
	void empty_data() {
		// given
		given(settlementRepository.findSettlementsForBatch(any(), any(), any()))
			.willReturn(Collections.emptyList());

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);
		verifyNoInteractions(paymentSettlementProcessor);
	}

	private Settlement createSettlement(Long id) {
		Settlement settlement = mock(Settlement.class);
		given(settlement.getId()).willReturn(id);
		return settlement;
	}
}