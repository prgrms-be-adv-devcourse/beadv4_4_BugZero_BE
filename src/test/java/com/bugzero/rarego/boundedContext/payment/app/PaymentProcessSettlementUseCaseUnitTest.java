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

import com.bugzero.rarego.boundedContext.payment.domain.Settlement;
import com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus;
import com.bugzero.rarego.boundedContext.payment.event.SettlementFinishedEvent;
import com.bugzero.rarego.boundedContext.payment.out.SettlementRepository;
import com.bugzero.rarego.global.event.EventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentProcessSettlementUseCaseUnitTest {

	@InjectMocks
	private PaymentProcessSettlementUseCase useCase;

	@Mock
	private PaymentSettlementProcessor paymentSettlementProcessor;

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private EventPublisher eventPublisher;

	@Test
	@DisplayName("정상 흐름: 2건 모두 성공 시 - 판매자 처리 2회 후 이벤트 발행 확인")
	void success_all() {
		// given
		Settlement s1 = createSettlement(1L);
		Settlement s2 = createSettlement(2L);
		List<Settlement> list = List.of(s1, s2);

		given(settlementRepository.findSettlementsForBatch(eq(SettlementStatus.READY), any(), anyInt()))
			.willReturn(list);

		given(paymentSettlementProcessor.processSellerDeposit(s1)).willReturn(true);
		given(paymentSettlementProcessor.processSellerDeposit(s2)).willReturn(true);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(2);

		// 1. 판매자 정산 처리 호출 검증
		verify(paymentSettlementProcessor).processSellerDeposit(s1);
		verify(paymentSettlementProcessor).processSellerDeposit(s2);

		// 2. [변경] 수수료 로직 직접 호출이 아닌, '이벤트 발행' 여부 검증
		verify(eventPublisher).publish(any(SettlementFinishedEvent.class));

		// (선택) 프로세서의 수수료 메서드는 UseCase에서 직접 호출되지 않음을 확인
		verify(paymentSettlementProcessor, never()).processFees(anyInt());
	}

	@Test
	@DisplayName("동시성 방어: 프로세서가 false를 반환하면 카운트되지 않지만, 이벤트는 발행됨")
	void skip_if_processor_returns_false() {
		// given
		Settlement s1 = createSettlement(1L);
		given(settlementRepository.findSettlementsForBatch(any(), any(), anyInt()))
			.willReturn(List.of(s1));

		// 이미 처리된 건 등으로 인해 false 반환
		given(paymentSettlementProcessor.processSellerDeposit(s1)).willReturn(false);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);

		// 처리 건수가 0이어도 마무리 이벤트는 발행되어야 함
		verify(eventPublisher).publish(any(SettlementFinishedEvent.class));
	}

	@Test
	@DisplayName("부분 성공: 1건 성공, 1건 실패(예외) 시 - 실패 처리 후 이벤트 발행됨")
	void partial_success() {
		// given
		Settlement successItem = createSettlement(1L);
		Settlement failItem = createSettlement(2L);

		given(settlementRepository.findSettlementsForBatch(any(), any(), anyInt()))
			.willReturn(List.of(successItem, failItem));

		given(paymentSettlementProcessor.processSellerDeposit(successItem)).willReturn(true);
		given(paymentSettlementProcessor.processSellerDeposit(failItem))
			.willThrow(new RuntimeException("Something wrong"));

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(1);

		// 실패 처리 검증
		verify(failItem).fail();

		// 예외가 발생했더라도 이벤트는 발행되어야 함
		verify(eventPublisher).publish(any(SettlementFinishedEvent.class));
	}

	@Test
	@DisplayName("빈 데이터: 데이터가 없어도 수수료 처리(잔여분)를 위해 이벤트는 발행되어야 함")
	void empty_data_but_publish_event() {
		// given
		given(settlementRepository.findSettlementsForBatch(any(), any(), anyInt()))
			.willReturn(Collections.emptyList());

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);

		// [중요] 빈 리스트여도 이벤트 발행 호출 확인
		verify(eventPublisher).publish(any(SettlementFinishedEvent.class));
	}

	private Settlement createSettlement(Long id) {
		Settlement settlement = mock(Settlement.class);
		lenient().when(settlement.getId()).thenReturn(id);
		return settlement;
	}
}