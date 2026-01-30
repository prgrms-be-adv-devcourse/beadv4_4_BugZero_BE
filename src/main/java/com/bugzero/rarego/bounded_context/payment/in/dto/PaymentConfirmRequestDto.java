package com.bugzero.rarego.bounded_context.payment.in.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequestDto(
	@NotBlank(message = "paymentKey는 필수입니다.")
	String paymentKey,

	@NotBlank(message = "orderId는 필수입니다.")
	String orderId,

	@NotNull(message = "결제 금액은 필수입니다.")
	@Min(value = 10000, message = "최소 결제 금액은 10,000원입니다.")
	Integer amount
) {
}
