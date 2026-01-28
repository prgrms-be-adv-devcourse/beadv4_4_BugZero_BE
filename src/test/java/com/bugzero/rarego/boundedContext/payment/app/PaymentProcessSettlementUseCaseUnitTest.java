package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

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

	@Test
	@DisplayName("정상 흐름: 판매자 정산 처리 후, 수수료 처리(processFees) 메서드를 호출한다")
	void success_all() {
		// given
		Settlement s1 = createSettlement(1L);
		Settlement s2 = createSettlement(2L);
		List<Settlement> settlements = List.of(s1, s2);

		// 1. 정산 대상 조회 Mock
		given(settlementRepository.findSettlementsForBatch(eq(SettlementStatus.READY), any(), any(Pageable.class)))
			.willReturn(settlements);

		// 2. 판매자 정산 성공 Mock
		given(paymentSettlementProcessor.processSellerDeposit(anyLong())).willReturn(true);

		// [주의] UseCase는 이제 processFees 내부 로직(Repository 조회 등)을 모릅니다.
		// 단순히 호출만 하는지 검증하면 됩니다.

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(2);

		// 판매자 정산 2회 호출 확인
		verify(paymentSettlementProcessor).processSellerDeposit(1L);
		verify(paymentSettlementProcessor).processSellerDeposit(2L);

		// [핵심] 수수료 처리 메서드 호출 확인 (시작과 끝에 두 번 호출될 수 있음)
		verify(paymentSettlementProcessor, atLeastOnce()).processFees(anyInt());
	}

	@Test
	@DisplayName("동시성 방어: 판매자 정산이 실패/스킵되어도, 수수료 처리는 독립적으로 시도(processFees)한다")
	void process_fees_independently() {
		// given
		Settlement s1 = createSettlement(1L);
		given(settlementRepository.findSettlementsForBatch(any(), any(), any()))
			.willReturn(List.of(s1));

		// 판매자 정산 실패 (이미 처리됨 등)
		given(paymentSettlementProcessor.processSellerDeposit(1L)).willReturn(false);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);

		// [중요] 판매자 정산이 실패하더라도, '이전에 쌓인 수수료'가 있을 수 있으므로
		// collectFees() -> processFees()는 반드시 호출되어야 합니다.
		verify(paymentSettlementProcessor, atLeastOnce()).processFees(anyInt());
	}

	@Test
	@DisplayName("부분 성공: 1건 성공, 1건 에러 발생 시 성공 카운트는 1이며, 수수료 처리는 시도된다")
	void partial_success() {
		// given
		Settlement successItem = createSettlement(1L);
		Settlement failItem = createSettlement(2L);

		given(settlementRepository.findSettlementsForBatch(eq(SettlementStatus.READY), any(), any(Pageable.class)))
			.willReturn(List.of(successItem, failItem));

		// 1번 성공, 2번 예외
		given(paymentSettlementProcessor.processSellerDeposit(1L)).willReturn(true);
		given(paymentSettlementProcessor.processSellerDeposit(2L)).willThrow(new RuntimeException("DB Lock"));

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(1);
		verify(paymentSettlementProcessor).fail(2L); // 실패 처리 호출 확인

		// 수수료 처리 시도 확인
		verify(paymentSettlementProcessor, atLeastOnce()).processFees(anyInt());
	}

	@Test
	@DisplayName("복구 로직: 정산할 건(Settlement)이 없어도 수수료 처리(processFees)는 시도한다")
	void process_pending_fees_when_no_settlements() {
		// given
		// 정산 대상 없음
		given(settlementRepository.findSettlementsForBatch(any(), any(), any()))
			.willReturn(Collections.emptyList());

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);

		// 정산 건이 없어도 processFees는 호출되어야 함 (복구 로직)
		verify(paymentSettlementProcessor).processFees(anyInt());
	}

	// --- Helper Methods ---
	private Settlement createSettlement(Long id) {
		Settlement settlement = mock(Settlement.class);
		lenient().when(settlement.getId()).thenReturn(id);
		return settlement;
	}
}