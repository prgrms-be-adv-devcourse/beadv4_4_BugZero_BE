package com.bugzero.rarego.shared.payment.out;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.exception.InternalApiErrorHandler;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

@Service
public class PaymentApiClient {
	private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
	private static final String CALLER_SERVICE_HEADER = "X-Caller-Service";

	private final RestClient restClient;
	private final InternalApiErrorHandler errorHandler;
	private final String internalSecret;
	// 나중에 spring.application.name에 각각의 서비스 이름을 제공함. 지금은 모듈화가 없어서 rarego
	private final String callerService;

	public PaymentApiClient(
		@Value("${custom.global.internalBackUrl}") String internalBackUrl,
		@Value("${spring.security.internal.secret}") String internalSecret,
		@Value("${spring.application.name:rarego}") String callerService,
		InternalApiErrorHandler errorHandler) {
		this.internalSecret = internalSecret;
		this.callerService = callerService;
		this.errorHandler = errorHandler;
		this.restClient = RestClient.builder()
			.baseUrl(internalBackUrl + "/api/v1/internal/payments")
			.defaultHeaders(headers -> headers.addAll(createInternalHeaders()))
			.build();
	}

	public DepositHoldResponseDto holdDeposit(int amount, String memberPublicId, Long auctionId) {
		DepositHoldRequestDto request = new DepositHoldRequestDto(amount, memberPublicId, auctionId);
		SuccessResponseDto<DepositHoldResponseDto> response = restClient.post()
			.uri("/deposits/hold")
			.body(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError, errorHandler::handle)
			.body(new ParameterizedTypeReference<>() {
			});
		if (response == null || response.data() == null) {
			throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
		}
		return response.data();
	}

	/**
	 * 처리 중인 주문이 있는지 확인
	 * PROCESSING 상태 주문이 있으면 true
	 */
	public boolean hasProcessingOrders(String publicId) {
		SuccessResponseDto<Boolean> response = restClient.get()
			.uri("/members/{publicId}/orders/processing", publicId)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
				throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
			})
			.body(new ParameterizedTypeReference<>() {
			});

		return response != null && Boolean.TRUE.equals(response.data());
	}

	private HttpHeaders createInternalHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(INTERNAL_SECRET_HEADER, internalSecret);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(CALLER_SERVICE_HEADER, callerService);
		return headers;
	}
}
