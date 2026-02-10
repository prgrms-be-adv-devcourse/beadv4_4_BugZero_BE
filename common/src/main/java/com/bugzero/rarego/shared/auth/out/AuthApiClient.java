package com.bugzero.rarego.shared.auth.out;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.global.exception.InternalApiErrorHandler;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.SystemAuthTokenProvider;

@Service
public class AuthApiClient {
	private final RestClient internalRestClient;
	private final InternalApiErrorHandler errorHandler;
	private final SystemAuthTokenProvider systemAuthTokenProvider;

	public AuthApiClient(
		@Value("${custom.global.internalBackUrl}") String internalBackUrl,
		InternalApiErrorHandler errorHandler,
		SystemAuthTokenProvider systemAuthTokenProvider) {
		this.errorHandler = errorHandler;
		this.internalRestClient = RestClient.builder()
			.baseUrl(internalBackUrl + "/api/v1/internal/auth")
			.build();
		this.systemAuthTokenProvider = systemAuthTokenProvider;
	}

	public void promoteSeller(String publicId) {
		internalRestClient.post()
			.uri("/accounts/{publicId}", publicId)
			.header("Authorization", "Bearer " + systemAuthTokenProvider.getSystemAccessToken())
			.retrieve()
			.onStatus(HttpStatusCode::isError,
				(httpRequest, httpResponse) -> errorHandler.handleWithDefault(httpRequest, httpResponse,
					ErrorType.AUTH_PROMOTE_SELLER_FAILED))
			.toBodilessEntity();
	}
}
