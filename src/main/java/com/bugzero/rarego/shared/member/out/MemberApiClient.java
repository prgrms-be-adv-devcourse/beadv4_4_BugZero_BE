package com.bugzero.rarego.shared.member.out;

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
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberWithdrawRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberWithdrawResponseDto;

import java.util.UUID;
import org.slf4j.MDC;

@Service
public class MemberApiClient {
	private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
	private static final String REQUEST_ID_HEADER = "X-Request-Id";
	private static final String CALLER_SERVICE_HEADER = "X-Caller-Service";

	private final RestClient internalRestClient;
	private final InternalApiErrorHandler errorHandler;

	@Value("${spring.security.internal.secret}")
	private String internalSecret;

	// 나중에 spring.application.name에 각각의 서비스 이름을 제공함. 지금은 모듈화가 없어서 rarego
	@Value("${spring.application.name:rarego}")
	private String callerService;

	public MemberApiClient(
		@Value("${custom.global.internalBackUrl}") String internalBackUrl,
		InternalApiErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
		this.internalRestClient = RestClient.builder()
			.baseUrl(internalBackUrl + "/api/v1/internal/members")
			.build();
	}

	public MemberJoinResponseDto join(String email) {
		MemberJoinRequestDto request = new MemberJoinRequestDto(email);
		SuccessResponseDto<MemberJoinResponseDto> response = internalRestClient.post()
			.uri("/me")
			.headers(headers -> headers.addAll(createInternalHeaders()))
			.body(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError,
				(httpRequest, httpResponse) -> errorHandler.handleWithDefault(httpRequest, httpResponse,
					ErrorType.MEMBER_JOIN_FAILED))

			.body(new ParameterizedTypeReference<>() {
			});
		if (response == null || response.data() == null) {
			throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
		}
		return response.data();
	}

	public String withdraw(String publicId) {
		MemberWithdrawRequestDto request = new MemberWithdrawRequestDto(publicId);
		SuccessResponseDto<MemberWithdrawResponseDto> response = internalRestClient.post()
			.uri("/withdraw")
			.headers(headers -> headers.addAll(createInternalHeaders()))
			.body(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError,
				(httpRequest, httpResponse) -> errorHandler.handleWithDefault(httpRequest, httpResponse,
					ErrorType.MEMBER_WITHDRAW_FAILED))
			.body(new ParameterizedTypeReference<>() {
			});
		if (response == null || response.data() == null) {
			throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
		}
		return response.data().publicId();
	}

	private HttpHeaders createInternalHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(INTERNAL_SECRET_HEADER, internalSecret);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(CALLER_SERVICE_HEADER, callerService);
		return headers;
	}
}
