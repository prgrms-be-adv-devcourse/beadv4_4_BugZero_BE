package com.bugzero.rarego.bounded_context.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.bugzero.rarego.bounded_context.payment.domain.Settlement;
import com.bugzero.rarego.bounded_context.payment.domain.SettlementStatus;
import com.bugzero.rarego.bounded_context.payment.out.SettlementRepository;

@ExtendWith(MockitoExtension.class)
class PaymentProcessSettlementUseCaseUnitTest {

	@InjectMocks
	private PaymentProcessSettlementUseCase useCase;

	@Mock
	private PaymentSettlementProcessor paymentSettlementProcessor;

	@Mock
	private SettlementRepository settlementRepository;

	@Test
	@DisplayName("정상 흐름: 2건 모두 성공(true 반환) 시 판매자 입금 2회, 시스템 수수료 합산 입금 1회 호출")
	void success_all() {
		// given
		Settlement s1 = createSettlement(1L, 1000);
		Settlement s2 = createSettlement(2L, 2000);
		List<Settlement> list = List.of(s1, s2);

		given(settlementRepository.findSettlementsForBatch(eq(SettlementStatus.READY), any(), any(Pageable.class)))
			.willReturn(list);

		// ✅ boolean 반환에 맞춰 stubbing 수정
		given(paymentSettlementProcessor.processSellerDeposit(anyLong())).willReturn(true);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(2);
		verify(paymentSettlementProcessor).processSellerDeposit(1L);
		verify(paymentSettlementProcessor).processSellerDeposit(2L);
		verify(paymentSettlementProcessor).processSystemDeposit(3000);
	}

	@Test
	@DisplayName("동시성 방어 검증: 프로세서가 false를 반환(이미 처리됨)하면 합산에서 제외됨")
	void skip_if_processor_returns_false() {
		// given
		Settlement s1 = createSettlement(1L, 1000);
		given(settlementRepository.findSettlementsForBatch(any(), any(), any()))
			.willReturn(List.of(s1));

		// ✅ 다른 스레드가 먼저 처리하여 false가 반환되는 상황 시뮬레이션
		given(paymentSettlementProcessor.processSellerDeposit(1L)).willReturn(false);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0); // 성공 카운트 0
		verify(paymentSettlementProcessor, never()).processSystemDeposit(anyInt()); // 수수료 입금 호출 안됨
	}

	@Test
	@DisplayName("부분 성공: 1건 성공(true), 1건 실패(Exception) 시 성공한 건만 입금됨")
	void partial_success() {
		// given
		Settlement successItem = createSettlement(1L, 1000);
		Settlement failItem = createSettlement(2L, 2000);

		given(settlementRepository.findSettlementsForBatch(eq(SettlementStatus.READY), any(), any(Pageable.class)))
			.willReturn(List.of(successItem, failItem));

		// ✅ 1번은 성공(true), 2번은 예외 발생
		given(paymentSettlementProcessor.processSellerDeposit(1L)).willReturn(true);
		given(paymentSettlementProcessor.processSellerDeposit(2L)).willThrow(new RuntimeException("DB Lock"));

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(1);
		verify(paymentSettlementProcessor).fail(2L);
		verify(paymentSettlementProcessor).processSystemDeposit(1000);
	}

	// empty_data, system_deposit_failure 테스트는 기존 로직(given/verify) 유지

	private Settlement createSettlement(Long id, int feeAmount) {
		Settlement settlement = mock(Settlement.class);
		lenient().when(settlement.getId()).thenReturn(id);
		lenient().when(settlement.getFeeAmount()).thenReturn(feeAmount);
		return settlement;
	}
}