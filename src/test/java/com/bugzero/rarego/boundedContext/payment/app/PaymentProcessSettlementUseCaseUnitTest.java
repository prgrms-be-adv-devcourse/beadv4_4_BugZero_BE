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

	private final Long SYSTEM_ID = 2L;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(useCase, "systemMemberId", SYSTEM_ID);
	}

	@Test
	@DisplayName("정상 흐름: 2건 모두 성공 시 수수료 합산 후 Processor에게 시스템 입금 위임")
	void success_all() {
		// given
		Settlement s1 = createSettlement(1L);
		Settlement s2 = createSettlement(2L);
		List<Settlement> list = List.of(s1, s2);

		given(settlementRepository.findAllByStatus(eq(SettlementStatus.READY), any(Pageable.class)))
			.willReturn(list);

		// Processor가 성공적으로 수수료(1000원)를 리턴한다고 가정
		given(paymentSettlementProcessor.process(anyLong())).willReturn(1000);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(2); // 2건 성공

		// Processor의 process가 2번 호출되었는지 검증
		verify(paymentSettlementProcessor, times(2)).process(anyLong());

		verify(paymentSettlementProcessor).depositSystemFee(eq(SYSTEM_ID), eq(2000));
	}

	@Test
	@DisplayName("부분 성공: 1건 성공, 1건 실패 시에도 중단되지 않고 진행되며, 성공분만 입금 위임")
	void partial_success() {
		// given
		Settlement successItem = createSettlement(1L);
		Settlement failItem = createSettlement(2L);

		given(settlementRepository.findAllByStatus(eq(SettlementStatus.READY), any(Pageable.class)))
			.willReturn(List.of(successItem, failItem));

		// 1번은 성공 (수수료 1000)
		given(paymentSettlementProcessor.process(1L)).willReturn(1000);

		// 2번은 예외 발생
		given(paymentSettlementProcessor.process(2L))
			.willThrow(new RuntimeException("DB Lock Error"));

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(1); // 1건만 성공 카운트

		// 실패한 2번 건에 대해 fail() 처리가 호출되었는지 확인
		verify(paymentSettlementProcessor).fail(2L);
		
		verify(paymentSettlementProcessor).depositSystemFee(eq(SYSTEM_ID), eq(1000));
	}

	@Test
	@DisplayName("데이터 없음: 0 반환 및 로직 스킵")
	void empty_data() {
		// given
		given(settlementRepository.findAllByStatus(any(), any()))
			.willReturn(Collections.emptyList());

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);

		// 프로세서와 아무런 상호작용이 없어야 함
		verifyNoInteractions(paymentSettlementProcessor);
	}

	private Settlement createSettlement(Long id) {
		Settlement settlement = mock(Settlement.class);
		given(settlement.getId()).willReturn(id);
		return settlement;
	}
}