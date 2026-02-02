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

import lombok.extern.slf4j.Slf4j;

@Service
public class MemberApiClient {
	private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
	private static final String CALLER_SERVICE_HEADER = "X-Caller-Service";

	private final RestClient internalRestClient;
	private final InternalApiErrorHandler errorHandler;
	private final String internalSecret;
	// 나중에 spring.application.name에 각각의 서비스 이름을 제공함. 지금은 모듈화가 없어서 rarego
	private final String callerService;

	public MemberApiClient(
		@Value("${custom.global.internalBackUrl}") String internalBackUrl,
		@Value("${spring.security.internal.secret}") String internalSecret,
		@Value("${spring.application.name:rarego}") String callerService,
		InternalApiErrorHandler errorHandler) {
		this.internalSecret = internalSecret;
		this.callerService = callerService;
		this.errorHandler = errorHandler;
		this.internalRestClient = RestClient.builder()
			.baseUrl(internalBackUrl + "/api/v1/internal/members")
			.defaultHeaders(headers -> headers.addAll(createInternalHeaders()))
			.build();
	}

	public MemberJoinResponseDto join(String email) {
		MemberJoinRequestDto request = new MemberJoinRequestDto(email);
		SuccessResponseDto<MemberJoinResponseDto> response = internalRestClient.post()
			.uri("/me")
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
