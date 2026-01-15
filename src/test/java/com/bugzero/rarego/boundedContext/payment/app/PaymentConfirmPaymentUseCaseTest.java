package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentStatus;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;
import com.bugzero.rarego.boundedContext.payment.out.TossPaymentsApiClient;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmPaymentUseCaseTest {

	@InjectMocks
	private PaymentConfirmPaymentUseCase useCase;

	@Mock
	private TossPaymentsApiClient tossApiClient;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentConfirmFinalizer paymentConfirmFinalizer;

	@Mock
	private PaymentSupport paymentSupport;

	@Test
	@DisplayName("결제 승인 성공: 모든 검증을 통과하고 최종 저장까지 호출되어야 한다")
	void confirmPayment_success() {
		// given
		Long memberId = 1L;
		String orderId = "ORDER_001";
		int amount = 10000;

		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("paymentKey", orderId, amount);

		// Mock 객체 생성
		PaymentMember member = PaymentMember.builder().id(memberId).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(amount)
			.status(PaymentStatus.PENDING)
			.build();

		TossPaymentsConfirmResponseDto tossResponse = new TossPaymentsConfirmResponseDto(orderId, "paymentKey", amount);
		PaymentConfirmResponseDto expectedResponse = new PaymentConfirmResponseDto(orderId, amount, 20000);

		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);
		given(tossApiClient.confirm(requestDto)).willReturn(tossResponse);
		given(paymentConfirmFinalizer.finalizePayment(payment, tossResponse)).willReturn(expectedResponse);

		// when
		PaymentConfirmResponseDto result = useCase.confirmPayment(memberId, requestDto);

		// then
		assertThat(result).isNotNull();
		assertThat(result.orderId()).isEqualTo(orderId);

		// 순서대로 호출되었는지 확인
		verify(paymentSupport).findPaymentByOrderId(orderId); // 1. 검증
		verify(tossApiClient).confirm(requestDto);        // 2. 외부 통신
		verify(paymentConfirmFinalizer).finalizePayment(payment, tossResponse); // 3. 최종 저장
	}

	@Test
	@DisplayName("실패: 요청한 사람이 결제 주인이 아니면 예외 발생 & 상태 FAILED 변경")
	void confirmPayment_fail_owner_mismatch() {
		// given
		Long realOwnerId = 1L;
		Long hackerId = 999L; // 다른 사람
		String orderId = "ORDER_001";

		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, 10000);

		PaymentMember owner = PaymentMember.builder().id(realOwnerId).build();
		Payment payment = Payment.builder()
			.member(owner)
			.status(PaymentStatus.PENDING)
			.build();

		// 1. validateRequest 단계 (Support 사용)
		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);

		// 2. 예외 발생 후 handlePaymentFailure 단계 (Repository 사용)
		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(hackerId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.PAYMENT_OWNER_MISMATCH);

		// 실패 처리 로직(fail())이 수행되었는지 확인
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED); // 상태 변경 확인
		verify(paymentRepository, times(1)).save(payment); // 저장 호출 확인
		verifyNoInteractions(tossApiClient); // 토스 API는 호출되지 않아야 함
	}

	@Test
	@DisplayName("실패: 결제 금액이 다르면 예외 발생")
	void confirmPayment_fail_amount_mismatch() {
		// given
		Long memberId = 1L;
		int realAmount = 10000;
		int fakeAmount = 5000; // 금액 조작
		String orderId = "ORDER_001";

		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, fakeAmount);

		PaymentMember member = PaymentMember.builder().id(memberId).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(realAmount) // DB에는 10000원
			.status(PaymentStatus.PENDING)
			.build();

		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);

		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_AMOUNT);

		// 상태가 FAILED로 변했는지 확인
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		verify(paymentRepository, times(1)).save(payment);
	}

	@Test
	@DisplayName("실패: 이미 처리된 주문이면 예외 발생 (FAILED 변경 로직 안 탐)")
	void confirmPayment_fail_already_processed() {
		// given
		Long memberId = 1L;
		String orderId = "ORDER_001";

		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, 10000);
		PaymentMember member = PaymentMember.builder().id(memberId).build();

		Payment payment = Payment.builder()
			.member(member)
			.amount(10000)
			.status(PaymentStatus.DONE) // 이미 완료된 상태
			.build();

		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);

		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_PROCESSED_PAYMENT);

		// Verify: save가 호출되지 않았는지 확인 (handlePaymentFailure 로직 상 PENDING 아니면 스킵함)
		verify(paymentRepository, never()).save(any());
	}

	@Test
	@DisplayName("실패: 토스 API 호출 중 에러 발생 시 상태를 FAILED로 변경해야 한다")
	void confirmPayment_fail_toss_api_error() {
		// given
		Long memberId = 1L;
		String orderId = "ORDER_001";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, 10000);

		PaymentMember member = PaymentMember.builder().id(memberId).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(10000)
			.status(PaymentStatus.PENDING)
			.build();

		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);

		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
		;

		// Toss API 호출 시 예외 발생시키기
		given(tossApiClient.confirm(any())).willThrow(new CustomException(ErrorType.PAYMENT_CONFIRM_FAILED));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.PAYMENT_CONFIRM_FAILED);

		// Verify: 예외가 터져도 handlePaymentFailure가 작동해서 FAILED로 바꿨는지 확인
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		verify(paymentRepository, times(1)).save(payment);
	}
}