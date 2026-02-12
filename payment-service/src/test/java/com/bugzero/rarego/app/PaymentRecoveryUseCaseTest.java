package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.domain.Payment;
import com.bugzero.rarego.domain.PaymentStatus;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.in.dto.TossPaymentsResponseDto;
import com.bugzero.rarego.out.PaymentRepository;
import com.bugzero.rarego.out.TossPaymentsApiClient;

@ExtendWith(MockitoExtension.class)
class PaymentRecoveryUseCaseTest {

	@InjectMocks
	private PaymentRecoveryUseCase paymentRecoveryUseCase;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private TossPaymentsApiClient tossApiClient;

	@Test
	@DisplayName("복구 대상이 없으면 로직이 바로 종료된다")
	void recoverPendingPayments_empty() {
		given(paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any()))
			.willReturn(List.of());

		paymentRecoveryUseCase.recoverPendingPayments();

		then(tossApiClient).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("Toss 상태가 DONE(결제완료)인 경우: 취소(Cancel) 요청 후 FAILED로 변경한다")
	void recoverPendingPayments_zombie_payment_done() {
		// given
		String orderId = "ORDER_DONE_001";
		String paymentKey = "key_123";
		Payment payment = createPendingPayment(orderId);

		given(paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any()))
			.willReturn(List.of(payment));

		// ✅ 수정됨: (orderId, paymentKey) 순서 확인!
		TossPaymentsResponseDto tossResponse = new TossPaymentsResponseDto(
			orderId, paymentKey, "DONE", 10000
		);
		given(tossApiClient.getPaymentByOrderId(orderId)).willReturn(tossResponse);

		// when
		paymentRecoveryUseCase.recoverPendingPayments();

		// then
		// 1. paymentKey("key_123")로 취소 요청이 갔는지 검증
		then(tossApiClient).should(times(1)).cancel(eq(paymentKey), anyString());

		// 2. 상태가 FAILED로 변경되고 저장되었는지 검증
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		then(paymentRepository).should(times(1)).save(payment);
	}

	@Test
	@DisplayName("Toss 상태가 WAITING_FOR_DEPOSIT(입금대기)인 경우: 취소(Cancel) 요청 후 FAILED로 변경한다")
	void recoverPendingPayments_zombie_payment_waiting() {
		// given
		String orderId = "ORDER_WAIT_001";
		String paymentKey = "key_456";
		Payment payment = createPendingPayment(orderId);

		given(paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any()))
			.willReturn(List.of(payment));

		// ✅ 수정됨: (orderId, paymentKey) 순서
		TossPaymentsResponseDto tossResponse = new TossPaymentsResponseDto(
			orderId, paymentKey, "WAITING_FOR_DEPOSIT", 10000
		);
		given(tossApiClient.getPaymentByOrderId(orderId)).willReturn(tossResponse);

		// when
		paymentRecoveryUseCase.recoverPendingPayments();

		// then
		then(tossApiClient).should(times(1)).cancel(eq(paymentKey), anyString());
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		then(paymentRepository).should(times(1)).save(payment);
	}

	@Test
	@DisplayName("Toss에서 404(내역없음)인 경우: 취소 요청 없이 단순 실패(FAILED) 처리한다")
	void recoverPendingPayments_not_found_in_toss() {
		// given
		String orderId = "ORDER_404";
		Payment payment = createPendingPayment(orderId);

		given(paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any()))
			.willReturn(List.of(payment));

		given(tossApiClient.getPaymentByOrderId(orderId))
			.willThrow(new CustomException(ErrorType.PAYMENT_NOT_FOUND_IN_TOSS));

		// when
		paymentRecoveryUseCase.recoverPendingPayments();

		// then
		then(tossApiClient).should(never()).cancel(anyString(), anyString());
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		then(paymentRepository).should(times(1)).save(payment);
	}

	@Test
	@DisplayName("Toss에서 이미 CANCELED 상태인 경우: 취소 요청 없이 우리 DB만 FAILED로 동기화한다")
	void recoverPendingPayments_already_canceled() {
		// given
		String orderId = "ORDER_CANCELED";
		Payment payment = createPendingPayment(orderId);

		given(paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any()))
			.willReturn(List.of(payment));

		// ✅ 수정됨: (orderId, paymentKey)
		TossPaymentsResponseDto tossResponse = new TossPaymentsResponseDto(
			orderId, "key_canceled", "CANCELED", 10000
		);
		given(tossApiClient.getPaymentByOrderId(orderId)).willReturn(tossResponse);

		// when
		paymentRecoveryUseCase.recoverPendingPayments();

		// then
		then(tossApiClient).should(never()).cancel(anyString(), anyString());
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		then(paymentRepository).should(times(1)).save(payment);
	}

	@Test
	@DisplayName("Toss 조회 중 알 수 없는 에러 발생 시: 해당 건은 스킵하고 저장하지 않는다")
	void recoverPendingPayments_api_error_skip() {
		// given
		String orderId = "ORDER_ERROR";
		Payment payment = createPendingPayment(orderId);

		given(paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any()))
			.willReturn(List.of(payment));

		given(tossApiClient.getPaymentByOrderId(orderId))
			.willThrow(new RuntimeException("Connection Refused"));

		// when
		paymentRecoveryUseCase.recoverPendingPayments();

		// then
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		then(paymentRepository).should(never()).save(payment);
	}

	@Test
	@DisplayName("복합 케이스: 첫 번째 건이 에러가 나도 두 번째 건은 정상 처리되어야 한다 (Loop 유지)")
	void recoverPendingPayments_loop_continues_on_error() {
		// given
		Payment errorPayment = createPendingPayment("ERROR_ITEM");
		Payment successPayment = createPendingPayment("SUCCESS_ITEM");

		given(paymentRepository.findAllByStatusAndCreatedAtBetween(any(), any(), any()))
			.willReturn(List.of(errorPayment, successPayment));

		// 1번 건: 에러
		given(tossApiClient.getPaymentByOrderId("ERROR_ITEM"))
			.willThrow(new RuntimeException("API Error"));

		// 2번 건: 정상 (DONE)
		// ✅ 수정됨: (orderId, paymentKey)
		TossPaymentsResponseDto tossResponse = new TossPaymentsResponseDto(
			"SUCCESS_ITEM", "key", "DONE", 10000
		);
		given(tossApiClient.getPaymentByOrderId("SUCCESS_ITEM")).willReturn(tossResponse);

		// when
		paymentRecoveryUseCase.recoverPendingPayments();

		// then
		// 1번 건: 저장 안 됨
		then(paymentRepository).should(never()).save(errorPayment);

		// 2번 건: 취소 호출 O (paymentKey="key"로 호출 확인), 저장 O
		then(tossApiClient).should(times(1)).cancel(eq("key"), anyString());
		then(paymentRepository).should(times(1)).save(successPayment);
		assertThat(successPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
	}

	private Payment createPendingPayment(String orderId) {
		return Payment.builder()
			.orderId(orderId)
			.amount(10000)
			.status(PaymentStatus.PENDING)
			.build();
	}
}
