package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.config.PaymentMetrics;
import com.bugzero.rarego.domain.Payment;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.out.PaymentRepository;
import com.bugzero.rarego.out.TossPaymentsApiClient;

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
	private final PaymentMetrics paymentMetrics;

	public PaymentConfirmResponseDto confirmPayment(String memberPublicId, PaymentConfirmRequestDto requestDto) {
		// 결제 시도 메트릭 기록
		paymentMetrics.incrementPaymentTotal();

		Long memberId = paymentSupport.findMemberByPublicId(memberPublicId).getId();
		Payment payment = paymentSupport.findPaymentByOrderId(requestDto.orderId());

		// 결제 정보 검증
		try {
			payment.validate(memberId, requestDto.amount());
		} catch (CustomException e) {
			paymentMetrics.incrementPaymentFailValidation();
			throw e;
		}

		TossPaymentsConfirmResponseDto tossResponse = null;

		try {
			// PG 승인 요청
			tossResponse = tossPaymentsApiClient.confirm(requestDto);

			// 결제 승인 완료 처리
			PaymentConfirmResponseDto result = paymentConfirmFinalizer.finalizePayment(payment, tossResponse);

			// 결제 성공 메트릭 기록
			paymentMetrics.incrementPaymentSuccess();
			paymentMetrics.recordPaymentAmount(requestDto.amount());

			return result;
		} catch (CustomException e) {
			if (e.getErrorType() == ErrorType.PAYMENT_CONFIRM_FAILED) {
				log.warn("PG 결제 승인 거절 - orderId: {}, reason: {}", requestDto.orderId(), e.getMessage());
				paymentMetrics.incrementPaymentFailPgReject();
				handleFail(payment);
			}

			throw e;
		} catch (Exception e) {
			// 토스 결제는 완료 됐으나 우리 서버 에러로 잔액이 안 올랐을 수 있음
			log.error("결제 승인 프로세스 중 시스템 에러 발생 - orderId: {}, error: {}", requestDto.orderId(), e.getMessage(), e);
			paymentMetrics.incrementPaymentFailSystem();

			if (tossResponse != null && tossResponse.paymentKey() != null) {
				try {
					tossPaymentsApiClient.cancel(tossResponse.paymentKey(), "서버 시스템 에러로 인한 취소 요청");
					log.info("결제 취소 요청 성공 - orderId: {}, paymentKey: {}", requestDto.orderId(),
						tossResponse.paymentKey());
				} catch (Exception e2) {
					log.error("결체 취소 요청 실패, 수동 처리 필요 - orderId: {}, paymentKey: {}, 원인: {}",
						requestDto.orderId(), tossResponse.paymentKey(), e2.getMessage(), e2);
				}
			}

			handleFail(payment);

			throw e;
		}
	}

	private void handleFail(Payment payment) {
		payment.fail();
		paymentRepository.save(payment); // 명시적 저장
	}
}
