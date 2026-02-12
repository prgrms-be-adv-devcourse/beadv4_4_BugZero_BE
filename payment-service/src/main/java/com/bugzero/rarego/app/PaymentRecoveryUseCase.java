package com.bugzero.rarego.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.domain.Payment;
import com.bugzero.rarego.domain.PaymentStatus;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.in.dto.TossPaymentsResponseDto;
import com.bugzero.rarego.out.PaymentRepository;
import com.bugzero.rarego.out.TossPaymentsApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecoveryUseCase {
	private final PaymentRepository paymentRepository;
	private final TossPaymentsApiClient tossPaymentsApiClient;

	public void recoverPendingPayments() {
		LocalDateTime end = LocalDateTime.now().minusMinutes(30);
		LocalDateTime start = LocalDateTime.now().minusDays(1);

		List<Payment> payments = paymentRepository.findAllByStatusAndCreatedAtBetween(PaymentStatus.PENDING, start,
			end);

		if (payments.isEmpty()) {
			return;
		}

		log.info("복구 대상 PENDING 결제: {}건", payments.size());

		for (Payment payment : payments) {
			try {
				processSingleRecovery(payment);
			} catch (Exception e) {
				// 한 건 실패해도 나머지는 계속 처리해야 함
				log.error("결제 복구 실패 - orderId: {}", payment.getOrderId(), e);
			}
		}
	}

	private void processSingleRecovery(Payment payment) {
		TossPaymentsResponseDto response;

		try {
			response = tossPaymentsApiClient.getPaymentByOrderId(payment.getOrderId());
		} catch (CustomException e) {
			if (e.getErrorType() == ErrorType.PAYMENT_NOT_FOUND_IN_TOSS) {
				log.info("결제 내역 없음 (단순 이탈) - orderId: {}", payment.getOrderId());
				markAsFailed(payment);
				return;
			}

			// 다음 스케줄러에서 재시도
			log.error("토스 조회 중 에러 발생 (스킵) - orderId: {}", payment.getOrderId(), e);
			return;
		}

		String status = response.status();

		if ("DONE".equals(status) || "WAITING_FOR_DEPOSIT".equals(status)) {
			log.warn("타임아웃된 결제 취소 처리 (Status: {}) - {}", status, payment.getOrderId());

			tossPaymentsApiClient.cancel(response.paymentKey(), "타임아웃 자동 취소");
		}

		markAsFailed(payment);
	}

	public void markAsFailed(Payment payment) {
		payment.fail();
		paymentRepository.save(payment); // 명시적 저장
	}
}
