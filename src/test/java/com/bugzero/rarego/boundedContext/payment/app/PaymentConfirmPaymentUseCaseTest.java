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
		String memberPublicId = "user-uuid"; // Long -> String 변경
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

		// [추가] PublicId -> Member 조회 Mocking
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);

		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);
		given(tossApiClient.confirm(requestDto)).willReturn(tossResponse);
		given(paymentConfirmFinalizer.finalizePayment(payment, tossResponse)).willReturn(expectedResponse);

		// when
		PaymentConfirmResponseDto result = useCase.confirmPayment(memberPublicId, requestDto);

		// then
		assertThat(result).isNotNull();
		assertThat(result.orderId()).isEqualTo(orderId);

		// 순서대로 호출되었는지 확인
		verify(paymentSupport).findMemberByPublicId(memberPublicId); // 0. 멤버 조회 확인
		verify(paymentSupport).findPaymentByOrderId(orderId);        // 1. 주문 조회 확인
		verify(tossApiClient).confirm(requestDto);                   // 2. 외부 통신 확인
		verify(paymentConfirmFinalizer).finalizePayment(payment, tossResponse); // 3. 최종 저장 확인
	}

	@Test
	@DisplayName("실패: 요청한 사람이 결제 주인이 아니면 예외 발생 & 상태 FAILED 변경")
	void confirmPayment_fail_owner_mismatch() {
		// given
		String hackerPublicId = "hacker-uuid"; // 해커의 ID
		Long hackerInternalId = 999L;

		Long realOwnerId = 1L;
		String orderId = "ORDER_001";

		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, 10000);

		// 해커 멤버 (요청자)
		PaymentMember hacker = PaymentMember.builder().id(hackerInternalId).build();

		// 진짜 주인 멤버
		PaymentMember owner = PaymentMember.builder().id(realOwnerId).build();

		Payment payment = Payment.builder()
			.member(owner) // 주문 주인은 owner
			.status(PaymentStatus.PENDING)
			.build();

		// [추가] 해커 조회
		given(paymentSupport.findMemberByPublicId(hackerPublicId)).willReturn(hacker);

		// 1. validateRequest 단계 (Support 사용)
		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);

		// 2. 예외 발생 후 handlePaymentFailure 단계 (Repository 사용)
		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(hackerPublicId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.PAYMENT_OWNER_MISMATCH);

		// 실패 처리 로직(fail())이 수행되었는지 확인
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		verify(paymentRepository, times(1)).save(payment);
		verifyNoInteractions(tossApiClient);
	}

	@Test
	@DisplayName("실패: 결제 금액이 다르면 예외 발생")
	void confirmPayment_fail_amount_mismatch() {
		// given
		String memberPublicId = "user-uuid";
		Long memberId = 1L;
		int realAmount = 10000;
		int fakeAmount = 5000;
		String orderId = "ORDER_001";

		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, fakeAmount);

		PaymentMember member = PaymentMember.builder().id(memberId).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(realAmount)
			.status(PaymentStatus.PENDING)
			.build();

		// [추가] 멤버 조회
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);

		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);
		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberPublicId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_AMOUNT);

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		verify(paymentRepository, times(1)).save(payment);
	}

	@Test
	@DisplayName("실패: 이미 처리된 주문이면 예외 발생 (FAILED 변경 로직 안 탐)")
	void confirmPayment_fail_already_processed() {
		// given
		String memberPublicId = "user-uuid";
		Long memberId = 1L;
		String orderId = "ORDER_001";

		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, 10000);
		PaymentMember member = PaymentMember.builder().id(memberId).build();

		Payment payment = Payment.builder()
			.member(member)
			.amount(10000)
			.status(PaymentStatus.DONE)
			.build();

		// [추가] 멤버 조회
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);

		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);
		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberPublicId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_PROCESSED_PAYMENT);

		verify(paymentRepository, never()).save(any());
	}

	@Test
	@DisplayName("실패: 토스 API 호출 중 에러 발생 시 상태를 FAILED로 변경해야 한다")
	void confirmPayment_fail_toss_api_error() {
		// given
		String memberPublicId = "user-uuid";
		Long memberId = 1L;
		String orderId = "ORDER_001";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", orderId, 10000);

		PaymentMember member = PaymentMember.builder().id(memberId).build();
		Payment payment = Payment.builder()
			.member(member)
			.amount(10000)
			.status(PaymentStatus.PENDING)
			.build();

		// [추가] 멤버 조회
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);

		given(paymentSupport.findPaymentByOrderId(orderId)).willReturn(payment);
		given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

		// Toss API 호출 시 예외 발생시키기
		given(tossApiClient.confirm(any())).willThrow(new CustomException(ErrorType.PAYMENT_CONFIRM_FAILED));

		// when & then
		assertThatThrownBy(() -> useCase.confirmPayment(memberPublicId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.PAYMENT_CONFIRM_FAILED);

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		verify(paymentRepository, times(1)).save(payment);
	}
}
