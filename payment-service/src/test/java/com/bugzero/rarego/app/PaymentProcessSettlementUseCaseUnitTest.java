package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.domain.PaymentMember;
import com.bugzero.rarego.domain.Settlement;
import com.bugzero.rarego.domain.SettlementStatus;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.out.SettlementRepository;
import com.bugzero.rarego.shared.payment.event.SettlementFinishedEvent;

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
	@DisplayName("정상 흐름: 2건 모두 성공 시 - 판매자 처리 2회 후 결과가 담긴 이벤트 발행")
	void success_all() {
		// given
		Settlement s1 = createMockSettlement(1L);
		Settlement s2 = createMockSettlement(2L);
		List<Settlement> list = List.of(s1, s2);

		given(settlementRepository.findSettlementsForBatch(eq(SettlementStatus.READY), any(), anyInt()))
			.willReturn(list);

		given(paymentSettlementProcessor.processSellerDeposit(s1)).willReturn(true);
		given(paymentSettlementProcessor.processSellerDeposit(s2)).willReturn(true);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(2);

		verify(paymentSettlementProcessor).processSellerDeposit(s1);
		verify(paymentSettlementProcessor).processSellerDeposit(s2);

		// [검증 수정] ArgumentCaptor를 사용하여 이벤트 내부 데이터 검증
		ArgumentCaptor<SettlementFinishedEvent> eventCaptor = ArgumentCaptor.forClass(SettlementFinishedEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());

		SettlementFinishedEvent event = eventCaptor.getValue();
		assertThat(event.settlements()).hasSize(2); // DTO가 2개 담겼는지 확인
		assertThat(event.settlements().get(0).id()).isEqualTo(1L);
		assertThat(event.settlements().get(1).id()).isEqualTo(2L);
	}

	@Test
	@DisplayName("동시성 방어: 프로세서가 false를 반환하면 카운트되지 않고 이벤트 리스트에도 포함되지 않음")
	void skip_if_processor_returns_false() {
		// given
		Settlement s1 = createMockSettlement(1L); // Mock 생성은 하지만
		given(settlementRepository.findSettlementsForBatch(any(), any(), anyInt()))
			.willReturn(List.of(s1));

		// 프로세서가 실패(false) 반환
		given(paymentSettlementProcessor.processSellerDeposit(s1)).willReturn(false);

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);

		// 이벤트는 발행되지만, 리스트는 비어있어야 함 (성공한 게 없으므로)
		ArgumentCaptor<SettlementFinishedEvent> eventCaptor = ArgumentCaptor.forClass(SettlementFinishedEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());

		assertThat(eventCaptor.getValue().settlements()).isEmpty();
	}

	@Test
	@DisplayName("부분 성공: 1건 성공, 1건 실패(예외) 시 - 실패 처리 후 성공한 건만 이벤트에 담김")
	void partial_success() {
		// given
		Settlement successItem = createMockSettlement(1L);
		Settlement failItem = createMockSettlement(2L); // 실패하는 건도 Mock 기본 설정은 해둠

		given(settlementRepository.findSettlementsForBatch(any(), any(), anyInt()))
			.willReturn(List.of(successItem, failItem));

		given(paymentSettlementProcessor.processSellerDeposit(successItem)).willReturn(true);
		given(paymentSettlementProcessor.processSellerDeposit(failItem))
			.willThrow(new RuntimeException("Something wrong"));

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(1);

		verify(failItem).fail();

		// 이벤트 검증: 성공한 1건만 들어있어야 함
		ArgumentCaptor<SettlementFinishedEvent> eventCaptor = ArgumentCaptor.forClass(SettlementFinishedEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());

		assertThat(eventCaptor.getValue().settlements()).hasSize(1);
		assertThat(eventCaptor.getValue().settlements().get(0).id()).isEqualTo(1L);
	}

	@Test
	@DisplayName("빈 데이터: 데이터가 없어도 빈 리스트 이벤트가 발행되어야 함")
	void empty_data_but_publish_event() {
		// given
		given(settlementRepository.findSettlementsForBatch(any(), any(), anyInt()))
			.willReturn(Collections.emptyList());

		// when
		int count = useCase.processSettlements(10);

		// then
		assertThat(count).isEqualTo(0);

		ArgumentCaptor<SettlementFinishedEvent> eventCaptor = ArgumentCaptor.forClass(SettlementFinishedEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());

		assertThat(eventCaptor.getValue().settlements()).isEmpty();
	}

	// [헬퍼 메서드 수정] DTO 변환 과정에서 호출되는 메서드들을 Stubbing 해야 함
	private Settlement createMockSettlement(Long id) {
		Settlement settlement = mock(Settlement.class);
		PaymentMember seller = mock(PaymentMember.class);

		// 기본 ID 설정
		lenient().when(settlement.getId()).thenReturn(id);

		// DTO 변환 시 호출되는 연관 관계 및 필드 Stubbing
		// settlement.getSeller().getId() 호출 대응
		lenient().when(seller.getId()).thenReturn(id * 10);
		lenient().when(settlement.getSeller()).thenReturn(seller);

		// 기타 필드들
		lenient().when(settlement.getAuctionId()).thenReturn(id * 100);
		lenient().when(settlement.getSalesAmount()).thenReturn(10000);
		lenient().when(settlement.getFeeAmount()).thenReturn(1000);
		lenient().when(settlement.getSettlementAmount()).thenReturn(9000);
		lenient().when(settlement.getStatus()).thenReturn(SettlementStatus.READY); // .name() 호출 대응

		return settlement;
	}
}