package com.bugzero.rarego.boundedContext.payment.in;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "결제 관련 API")
public class PaymentController {
	private final PaymentFacade paymentFacade;

	@Operation(summary = "결제 요청", description = "토스페이먼츠 결제를 요청합니다")
	@PostMapping("/charges")
	public SuccessResponseDto<PaymentRequestResponseDto> requestPayment(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		// 현재는 테스트를 위해 Query Parameter로 받음
		@RequestParam Long memberId,
		@Valid @RequestBody PaymentRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.CREATED, paymentFacade.requestPayment(memberId, requestDto));
	}

	@Operation(summary = "결제 승인", description = "토스페이먼츠 결제 승인 후 지갑에 충전합니다")
	@PostMapping("/charges/confirm")
	public SuccessResponseDto<PaymentConfirmResponseDto> confirmPayment(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		// 현재는 테스트를 위해 Query Parameter로 받음
		@RequestParam Long memberId,
		@Valid @RequestBody PaymentConfirmRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.OK, paymentFacade.confirmPayment(memberId, requestDto));
	}

	@Operation(summary = "낙찰 결제", description = "낙찰자가 최종 결제를 완료합니다 (보증금 + 잔금)")
	@PostMapping("/auctions/{auctionId}")
	public SuccessResponseDto<AuctionFinalPaymentResponseDto> auctionFinalPayment(
		// TODO: 추후 인증 구현시 @AuthenticationPrincipal로 변경 필요
		@RequestParam Long memberId,
		@PathVariable Long auctionId,
		@Valid @RequestBody AuctionFinalPaymentRequestDto requestDto
	) {
		return SuccessResponseDto.from(SuccessType.OK, paymentFacade.auctionFinalPayment(memberId, auctionId, requestDto));
	}
}
