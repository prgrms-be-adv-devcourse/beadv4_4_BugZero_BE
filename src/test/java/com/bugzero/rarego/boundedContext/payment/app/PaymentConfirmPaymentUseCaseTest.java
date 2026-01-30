package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

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
		String memberPublicId = "user-uuid";
		Long memberId = 1L;
		String orderId = "ORDER_001";
		int amount = 10000;

		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("paymentKey", orderId, amount);
		PaymentMember member = PaymentMember.builder().id(memberId).build();

		// spy를 사용하면 실제 validate() 메서드가 호출되는 것을 지켜볼 수 있습니다.
		Payment payment = spy(Payment.builder()
			.member(member)
			.amount(amount)
			.status(PaymentStatus.PENDING)
			.build());

		TossPaymentsConfirmResponseDto tossResponse = new TossPaymentsConfirmResponseDto(orderId, "paymentKey", amount);
		PaymentConfirmResponseDto expectedResponse = new PaymentConfirmResponseDto(orderId, amount, 20000);

		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);
		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);
		given(tossApiClient.confirm(requestDto)).willReturn(tossResponse);
		given(paymentConfirmFinalizer.finalizePayment(payment, tossResponse)).willReturn(expectedResponse);

		// when
		PaymentConfirmResponseDto result = useCase.confirmPayment(memberPublicId, requestDto);

		// then
		assertThat(result).isNotNull();
		verify(payment).validate(memberId, amount); // 엔티티의 validate 호출 확인
		verify(paymentConfirmFinalizer).finalizePayment(payment, tossResponse);
	}

	@Test
	@DisplayName("실패: 검증 단계(금액 미달 등)에서 에러 발생 시 handleFail을 타지 않고 바로 예외를 던진다")
	void confirmPayment_fail_validation() {
		// given
		String memberPublicId = "user-uuid";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "ORDER_001", 5000); // 잘못된 금액

		PaymentMember member = PaymentMember.builder().id(1L).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(10000) // 실제 금액은 10000
			.status(PaymentStatus.PENDING)
			.build();

		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);
		given(paymentSupport.findPaymentByOrderId(anyString())).willReturn(payment);

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberPublicId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_AMOUNT);

		// 검증 실패 시 catch 블록으로 가지 않으므로 fail()과 save()가 호출되지 않아야 함
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		verify(paymentRepository, never()).save(any());
	}

	@Test
	@DisplayName("실패: 토스 API 호출 중 PAYMENT_CONFIRM_FAILED 발생 시 상태를 FAILED로 변경하고 저장한다")
	void confirmPayment_fail_toss_api_error() {
		// given
		String memberPublicId = "user-uuid";
		String orderId = "ORDER_001";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, 10000);

		PaymentMember member = PaymentMember.builder().id(1L).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(10000)
			.status(PaymentStatus.PENDING)
			.build();

		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);
		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);

		// Toss API 에러 발생
		given(tossApiClient.confirm(any())).willThrow(new CustomException(ErrorType.PAYMENT_CONFIRM_FAILED));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberPublicId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.PAYMENT_CONFIRM_FAILED);

		// then: handleFail 로직 확인
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		verify(paymentRepository).save(payment);
	}

	@Test
	@DisplayName("실패: 예상치 못한 시스템 에러(Exception) 발생 시에도 FAILED로 변경하고 저장한다")
	void confirmPayment_fail_unexpected_system_error() {
		// given
		String memberPublicId = "user-uuid";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "ORDER_001", 10000);

		PaymentMember member = PaymentMember.builder().id(1L).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(10000)
			.status(PaymentStatus.PENDING)
			.build();

		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);
		given(paymentSupport.findPaymentByOrderId(anyString())).willReturn(payment);

		// finalizePayment 도중 런타임 에러 발생 (DB 연결 끊김 등)
		given(tossApiClient.confirm(any())).willReturn(mock(TossPaymentsConfirmResponseDto.class));
		given(paymentConfirmFinalizer.finalizePayment(any(), any())).willThrow(new RuntimeException("DB Error"));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberPublicId, requestDto))
			.isInstanceOf(RuntimeException.class);

		// then: Exception catch 블록에서 handleFail 호출 확인
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		verify(paymentRepository).save(payment);
	}

	@Test
	@DisplayName("실패: 결제 승인은 성공했으나 내부 시스템 에러 발생 시, 보상 트랜잭션(cancel)이 호출되어야 한다")
	void confirmPayment_fail_system_error_trigger_compensation() {
		// given
		String memberPublicId = "user-uuid";
		String orderId = "ORDER_001";
		String paymentKey = "test_payment_key";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto(paymentKey, orderId, 10000);

		PaymentMember member = PaymentMember.builder().id(1L).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(10000)
			.status(PaymentStatus.PENDING)
			.build();

		TossPaymentsConfirmResponseDto tossResponse = new TossPaymentsConfirmResponseDto(orderId, paymentKey, 10000);

		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);
		given(paymentSupport.findPaymentByOrderId(anyString())).willReturn(payment);

		// 1. Toss API는 성공
		given(tossApiClient.confirm(any())).willReturn(tossResponse);
		// 2. 내부 로직(DB반영)에서 에러 발생
		given(paymentConfirmFinalizer.finalizePayment(any(), any())).willThrow(new RuntimeException("DB Connection Error"));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberPublicId, requestDto))
			.isInstanceOf(RuntimeException.class);

		// then
		// 1. 상태가 FAILED로 변경되어야 함
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		verify(paymentRepository).save(payment);

		// [핵심] 2. 보상 트랜잭션(취소 API)이 호출되었는지 검증
		verify(tossApiClient).cancel(eq(paymentKey), anyString());
	}
}
