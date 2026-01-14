package com.bugzero.rarego.boundedContext.payment.in.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentRequestDto(
	@NotNull(message = "결제 금액은 필수입니다.")
	@Min(value = 10000, message = "최소 결제 금액은 10,000원입니다.")
	Integer amount
) {
}
