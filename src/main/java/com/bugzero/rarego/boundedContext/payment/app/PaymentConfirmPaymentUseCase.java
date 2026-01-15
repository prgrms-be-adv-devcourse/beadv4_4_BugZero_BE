package com.bugzero.rarego.boundedContext.payment.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentStatus;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;
import com.bugzero.rarego.boundedContext.payment.out.TossPaymentsApiClient;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfirmPaymentUseCase {
	private final TossPaymentsApiClient tossPaymentsApiClient;
	private final PaymentRepository paymentRepository;
	private final PaymentConfirmFinalizer paymentConfirmFinalizer;

	public PaymentConfirmResponseDto confirmPayment(Long memberId, PaymentConfirmRequestDto requestDto) {
		try {
			Payment payment = validateRequest(memberId, requestDto);

			TossPaymentsConfirmResponseDto tossResponse = tossPaymentsApiClient.confirm(requestDto);

			return paymentConfirmFinalizer.finalizePayment(payment, tossResponse);
		} catch (Exception e) { // 결제 승인 중 에러 발생 시 결제 상태를 실패로 변경
			handlePaymentFailure(requestDto.orderId());
			throw e;
		}
	}

	private Payment validateRequest(Long memberId, PaymentConfirmRequestDto requestDto) {
		Payment payment = paymentRepository.findByOrderId(requestDto.orderId())
			.orElseThrow(() -> new CustomException(ErrorType.PAYMENT_NOT_FOUND));

		// 내 주문이 맞는지 확인
		if (!payment.getMember().getId().equals(memberId)) {
			throw new CustomException(ErrorType.PAYMENT_OWNER_MISMATCH);
		}

		// 금액 위변조 확인
		if (payment.getAmount() != requestDto.amount()) {
			throw new CustomException(ErrorType.INVALID_PAYMENT_AMOUNT);
		}

		// 이미 처리된 주문인지 확인
		if (payment.getStatus() != PaymentStatus.PENDING) {
			throw new CustomException(ErrorType.ALREADY_PROCESSED_PAYMENT);
		}

		return payment;
	}

	private void handlePaymentFailure(String orderId) {
		paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
			if (payment.getStatus() == PaymentStatus.PENDING) {
				payment.fail();
				paymentRepository.save(payment); // 트랜잭션 처리가 되어있지 않으므로 명시적으로 저장
			}
		});
	}
}
