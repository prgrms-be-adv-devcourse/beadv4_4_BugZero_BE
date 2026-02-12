package com.bugzero.rarego.out;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.in.dto.TossPaymentsResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentsApiClient {
	private final RestClient tossPaymentsRestClient;

	public TossPaymentsResponseDto confirm(PaymentConfirmRequestDto requestDto) {
		return tossPaymentsRestClient.post()
			.uri("/confirm")
			.body(requestDto)
			.retrieve()
			.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
				String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);

				log.error("토스 결제 승인 실패 - 상태코드: {}, 내용: {}", response.getStatusCode(), errorBody);

				throw new CustomException(ErrorType.PAYMENT_CONFIRM_FAILED);
			})
			.body(TossPaymentsResponseDto.class);
	}

	public TossPaymentsResponseDto getPaymentByOrderId(String orderId) {
		return tossPaymentsRestClient.get()
			.uri("/orders/" + orderId)
			.retrieve()
			.onStatus(status -> status.value() == 404, (req, res) -> {
				// 404: 결제 내역 없음 -> null 처리를 위해 예외 던짐
				throw new CustomException(ErrorType.PAYMENT_NOT_FOUND_IN_TOSS);
			})
			.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
				String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
				log.error("토스 결제 조회 실패 - orderId: {}, 내용: {}", orderId, errorBody);
				throw new CustomException(ErrorType.PAYMENT_LOOKUP_FAILED);
			})
			.body(TossPaymentsResponseDto.class);
	}

	public void cancel(String paymentKey, String reason) {
		tossPaymentsRestClient.post()
			.uri("/{paymentKey}/cancel", paymentKey)
			.body(Map.of("cancelReason", reason))
			.retrieve()
			.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
				String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
				log.error("토스 결제 취소 실패 - paymentKey: {}, 네용: {}", paymentKey, errorBody);
				throw new CustomException(ErrorType.PAYMENT_CANCEL_FAILED);
			})
			.toBodilessEntity();
	}
}
