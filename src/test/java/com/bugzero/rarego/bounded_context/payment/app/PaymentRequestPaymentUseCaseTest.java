package com.bugzero.rarego.bounded_context.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.bounded_context.payment.domain.Payment;
import com.bugzero.rarego.bounded_context.payment.domain.PaymentMember;
import com.bugzero.rarego.bounded_context.payment.domain.PaymentStatus;
import com.bugzero.rarego.bounded_context.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.bounded_context.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.bounded_context.payment.out.PaymentRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class PaymentRequestPaymentUseCaseTest {
	@InjectMocks
	private PaymentRequestPaymentUseCase useCase;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentSupport paymentSupport;

	@Test
	@DisplayName("예치금 결제 요청 성공 테스트")
	void requestPayment_success() {
		// given
		String memberPublicId = "uuid-user-1234";
		Integer amount = 10000;
		PaymentRequestDto requestDto = new PaymentRequestDto(amount);

		// 가짜 멤버 객체 생성
		PaymentMember member = PaymentMember.builder()
			.id(1L)
			.build();

		// [변경] paymentSupport.findMemberByPublicId 호출 스텁 설정
		given(paymentSupport.findMemberByPublicId(memberPublicId)).willReturn(member);

		// when
		PaymentRequestResponseDto response = useCase.requestPayment(memberPublicId, requestDto);

		// then
		// 1. Repository 저장 호출 검증
		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();

		// 2. 저장된 객체 검증
		assertThat(savedPayment.getMember()).isEqualTo(member);
		assertThat(savedPayment.getAmount()).isEqualTo(amount);
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(savedPayment.getOrderId()).isNotNull(); // UUID 생성 확인

		// 3. 반환값 검증
		assertThat(response.amount()).isEqualTo(amount);
	}

	@Test
	@DisplayName("결제 요청 실패: 존재하지 않는 회원")
	void requestPayment_fail_member_not_found() {
		// given
		String memberPublicId = "unknown-user"; // Long -> String 변경
		Integer amount = 10000;
		PaymentRequestDto requestDto = new PaymentRequestDto(amount);

		// [변경] paymentSupport.findMemberByPublicId 호출 시 예외 발생 설정
		given(paymentSupport.findMemberByPublicId(memberPublicId))
			.willThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> useCase.requestPayment(memberPublicId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.MEMBER_NOT_FOUND);

		// 예외 발생 시 저장은 호출되지 않아야 함
		verify(paymentRepository, never()).save(any());
	}
}
