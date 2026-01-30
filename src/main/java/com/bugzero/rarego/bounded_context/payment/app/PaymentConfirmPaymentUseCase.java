package com.bugzero.rarego.bounded_context.payment.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.bounded_context.payment.domain.Payment;
import com.bugzero.rarego.bounded_context.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.bounded_context.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.bounded_context.payment.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.bounded_context.payment.out.PaymentRepository;
import com.bugzero.rarego.bounded_context.payment.out.TossPaymentsApiClient;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfirmPaymentUseCase {
	private final TossPaymentsApiClient tossPaymentsApiClient;
	private final PaymentConfirmFinalizer paymentConfirmFinalizer;
	private final PaymentSupport paymentSupport;
	private final PaymentRepository paymentRepository;

	public PaymentConfirmResponseDto confirmPayment(String memberPublicId, PaymentConfirmRequestDto requestDto) {
		Long memberId = paymentSupport.findMemberByPublicId(memberPublicId).getId();
		Payment payment = paymentSupport.findPaymentByOrderId(requestDto.orderId());

		// 결제 정보 검증
		payment.validate(memberId, requestDto.amount());

		try {
			// PG 승인 요청
			TossPaymentsConfirmResponseDto tossResponse = tossPaymentsApiClient.confirm(requestDto);

			// 결제 승인 완료 처리
			return paymentConfirmFinalizer.finalizePayment(payment, tossResponse);
		} catch (CustomException e) {
			if (e.getErrorType() == ErrorType.PAYMENT_CONFIRM_FAILED) {
				log.warn("PG 결제 승인 거절 - orderId: {}, reason: {}", requestDto.orderId(), e.getMessage());
				handleFail(payment);
			}

			throw e;
		} catch (Exception e) {
			// 토스 결제는 완료 됐으나 우리 서버 에러로 잔액이 안 올랐을 수 있음
			log.error("결제 승인 프로세스 중 시스템 에러 발생 - orderId: {}, error: {}", requestDto.orderId(), e.getMessage(), e);

			// TODO: 데이터 정합성 해결 - 보상 트랜잭션 or 알림 후 수동 처리

			handleFail(payment);

			throw e;
		}
	}

	private void handleFail(Payment payment) {
		payment.fail();
		paymentRepository.save(payment); // 명시적 저장
	}
}
