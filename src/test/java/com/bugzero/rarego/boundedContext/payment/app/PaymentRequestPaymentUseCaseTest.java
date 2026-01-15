package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentStatus;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;
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
		Long memberId = 1L;
		Integer amount = 10000;
		PaymentRequestDto requestDto = new PaymentRequestDto(amount);

		// 가짜 멤버 객체 생성
		PaymentMember member = PaymentMember.builder().id(memberId).build();

		// repository.findById 호출 시 가짜 멤버 리턴하도록 설정
		given(paymentSupport.findMemberById(memberId)).willReturn(member);

		// when
		PaymentRequestResponseDto response = useCase.requestPayment(memberId, requestDto);

		// then
		// 1. 실제로 DB 저장 메서드가 호출되었는지 검증 & 저장되려던 객체 포획(Capture)
		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();

		// 2. 내부 로직 검증
		assertThat(savedPayment.getMember()).isEqualTo(member);
		assertThat(savedPayment.getAmount()).isEqualTo(amount);
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(savedPayment.getOrderId()).isNotNull();

		// 3. 반환값 검증
		assertThat(response.amount()).isEqualTo(amount);
	}

	@Test
	@DisplayName("결제 요청 실패: 존재하지 않는 회원")
	void requestPayment_fail_member_not_found() {
		// given
		Long memberId = 999L;
		Integer amount = 10000;
		PaymentRequestDto requestDto = new PaymentRequestDto(amount);

		// repository가 빈 값을 반환하도록 설정
		given(paymentSupport.findMemberById(memberId))
			.willThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> useCase.requestPayment(memberId, requestDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.MEMBER_NOT_FOUND);

		// 예외 발생 시 저장은 호출되지 않아야 함
		verify(paymentRepository, never()).save(any());
	}
}