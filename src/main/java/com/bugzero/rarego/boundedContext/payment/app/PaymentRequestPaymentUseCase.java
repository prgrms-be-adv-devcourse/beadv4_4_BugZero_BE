package com.bugzero.rarego.boundedContext.payment.app;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.Payment;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentStatus;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.boundedContext.payment.out.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentRequestPaymentUseCase {
	private final PaymentRepository paymentRepository;
	private final PaymentSupport paymentSupport;

	@Transactional
	public PaymentRequestResponseDto requestPayment(String memberPublicId, PaymentRequestDto requestDto) {
		PaymentMember member = paymentSupport.findMemberByPublicId(memberPublicId);

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
