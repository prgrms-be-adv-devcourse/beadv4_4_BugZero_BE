package com.bugzero.rarego.boundedContext.payment.out;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.TossPaymentsConfirmResponseDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentsApiClient {
	private final RestClient tossPaymentsRestClient;

	public TossPaymentsConfirmResponseDto confirm(PaymentConfirmRequestDto requestDto) {
		return tossPaymentsRestClient.post()
			.uri("/confirm")
			.body(requestDto)
			.retrieve()
			.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
				String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);

				log.error("토스 결제 승인 실패 - 상태코드: {}, 내용: {}", response.getStatusCode(), errorBody);

				throw new CustomException(ErrorType.PAYMENT_CONFIRM_FAILED);
			})
			.body(TossPaymentsConfirmResponseDto.class);
	}
}
