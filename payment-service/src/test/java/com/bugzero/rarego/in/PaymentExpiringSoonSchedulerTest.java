package com.bugzero.rarego.in;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.app.PaymentFacade;
import com.bugzero.rarego.out.AuctionOrderApiClient;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

@ExtendWith(MockitoExtension.class)
class PaymentExpiringSoonSchedulerTest {

	@Mock
	private AuctionOrderApiClient auctionOrderApiClient;

	@Mock
	private PaymentFacade paymentFacade;

	@InjectMocks
	private PaymentExpiringSoonScheduler scheduler;

	@BeforeEach
	void setUp() {
		// @Value("${auction.payment-timeout-days:3}") 값을 주입
		ReflectionTestUtils.setField(scheduler, "paymentTimeoutDays", 3);
	}

	@Test
	@DisplayName("마감 임박 주문이 없으면 아무 동작도 하지 않고 종료된다")
	void checkExpiringSoon_empty() {
		// given
		// 첫 번째 조회에서 바로 빈 리스트 반환
		given(auctionOrderApiClient.findExpiringSoonOrders(any(), any()))
			.willReturn(new AuctionOrderApiClient.AuctionOrderSlice(Collections.emptyList(), false));

		// when
		scheduler.checkExpiringSoon();

		// then
		then(paymentFacade).should(never()).publishExpiringSoonEvent(any(), any());
		then(auctionOrderApiClient).should(never()).markAsNoticed(any());
	}

	@Test
	@DisplayName("마감 임박 주문이 있으면 이벤트를 발행하고 알림 완료 처리를 한다")
	void checkExpiringSoon_success() {
		// given
		AuctionOrderDto order1 = createOrderDto(1L);
		AuctionOrderDto order2 = createOrderDto(2L);

		// 1. 첫 번째 조회: 주문 2개 반환
		// 2. 두 번째 조회: 빈 리스트 반환 (루프 종료용)
		given(auctionOrderApiClient.findExpiringSoonOrders(any(), eq(PageRequest.of(0, 100))))
			.willReturn(new AuctionOrderApiClient.AuctionOrderSlice(List.of(order1, order2), true))
			.willReturn(new AuctionOrderApiClient.AuctionOrderSlice(Collections.emptyList(), false));

		// when
		scheduler.checkExpiringSoon();

		// then
		// 1. 이벤트 발행 검증 (총 2회)
		then(paymentFacade).should(times(1)).publishExpiringSoonEvent(eq(order1), any(LocalDateTime.class));
		then(paymentFacade).should(times(1)).publishExpiringSoonEvent(eq(order2), any(LocalDateTime.class));

		// 2. 상태 마킹 검증 (총 2회)
		then(auctionOrderApiClient).should(times(1)).markAsNoticed(order1.orderId());
		then(auctionOrderApiClient).should(times(1)).markAsNoticed(order2.orderId());
	}

	@Test
	@DisplayName("처리 중 예외가 발생해도 다음 주문 처리는 계속된다")
	void checkExpiringSoon_continueOnError() {
		// given
		// 1. 서로 다른 ID를 가진 객체 생성 확인
		AuctionOrderDto successOrder = createOrderDto(1L);
		AuctionOrderDto failOrder = createOrderDto(2L);
		AuctionOrderDto nextOrder = createOrderDto(3L);

		// 2. ApiClient Mocking
		given(auctionOrderApiClient.findExpiringSoonOrders(any(), any()))
			.willReturn(new AuctionOrderApiClient.AuctionOrderSlice(List.of(successOrder, failOrder, nextOrder), true))
			.willReturn(new AuctionOrderApiClient.AuctionOrderSlice(Collections.emptyList(), false));

		// 3. PaymentFacade Mocking (중요!)
		// failOrder(ID 2)일 때만 예외 발생
		lenient().doThrow(new RuntimeException("Kafka Error"))
			.when(paymentFacade).publishExpiringSoonEvent(eq(failOrder), any());

		// when
		scheduler.checkExpiringSoon();

		// then
		// 1번(성공)과 3번(성공)은 마킹이 호출되어야 함
		then(auctionOrderApiClient).should().markAsNoticed(1L); // successOrder
		then(auctionOrderApiClient).should().markAsNoticed(3L); // nextOrder

		// 2번(실패)은 마킹이 호출되지 않아야 함
		then(auctionOrderApiClient).should(never()).markAsNoticed(2L); // failOrder
	}

	@Test
	@DisplayName("조회 시 계산된 날짜 파라미터가 정상적으로 전달되는지 검증")
	void verify_date_calculation() {
		// given
		given(auctionOrderApiClient.findExpiringSoonOrders(any(), any()))
			.willReturn(new AuctionOrderApiClient.AuctionOrderSlice(Collections.emptyList(), false));

		ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

		// when
		scheduler.checkExpiringSoon();

		// then
		then(auctionOrderApiClient).should().findExpiringSoonOrders(dateCaptor.capture(), any());

		LocalDateTime capturedDate = dateCaptor.getValue();
		// 오늘 날짜 - 3일
		// 시간은 LocalTime.MAX (23:59:59...) 인지 확인
		LocalDateTime expectedDate = java.time.LocalDate.now().minusDays(3).atTime(java.time.LocalTime.MAX);

		// 초 단위 차이 무시하고 비교 (실행 속도 차이 고려)
		// 여기서는 날짜와 시간이 정확한지 AssertJ 등으로 검증 가능
		assertThat(capturedDate).isEqualToIgnoringNanos(expectedDate);
	}

	// 테스트용 DTO 생성 헬퍼
	private AuctionOrderDto createOrderDto(Long id) {
		// 필요한 필드만 채워서 생성 (Record라고 가정)
		return new AuctionOrderDto(
			id,       // orderId
			100L,     // auctionId
			200L,     // buyerId
			300L,     // sellerId
			10000,
			"PROCESSING",
			LocalDateTime.now()
		);
	}
}