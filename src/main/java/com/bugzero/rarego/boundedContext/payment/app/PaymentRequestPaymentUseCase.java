package com.bugzero.rarego.boundedContext.payment.app;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentStatus;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentRequestPaymentUseCase {
	private final PaymentMemberRepository paymentMemberRepository;
	private final PaymentRepository paymentRepository;

	@Transactional
	public PaymentRequestResponseDto requestPayment(Long memberId, PaymentRequestDto requestDto) {
		PaymentMember member = paymentMemberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

		String orderId = UUID.randomUUID().toString();

		Payment payment = Payment.builder()
			.member(member)
			.orderId(orderId)
			.amount(requestDto.amount())
			.status(PaymentStatus.PENDING)
			.build();

		paymentRepository.save(payment);

		return PaymentRequestResponseDto.from(payment);
	}
}
