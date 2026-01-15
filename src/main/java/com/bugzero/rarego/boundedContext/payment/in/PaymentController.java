package com.bugzero.rarego.boundedContext.payment.in;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
	private final PaymentFacade paymentFacade;

	@PostMapping("/charges")
	public SuccessResponseDto<PaymentRequestResponseDto> requestPayment(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		// 현재는 테스트를 위해 Query Parameter로 받음
		@RequestParam Long memberId,
		@Valid @RequestBody PaymentRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.CREATED, paymentFacade.requestPayment(memberId, requestDto));
	}

	@PostMapping("/charges/confirm")
	public SuccessResponseDto<PaymentConfirmResponseDto> confirmPayment(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		// 현재는 테스트를 위해 Query Parameter로 받음
		@RequestParam Long memberId,
		@Valid @RequestBody PaymentConfirmRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.OK, paymentFacade.confirmPayment(memberId, requestDto));
	}
}
