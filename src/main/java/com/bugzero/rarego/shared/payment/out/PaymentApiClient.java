package com.bugzero.rarego.shared.payment.out;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
    private final RestClient restClient;
    private final InternalApiErrorHandler errorHandler;

    public PaymentApiClient(
            @Value("${custom.global.internalBackUrl}") String internalBackUrl,
            InternalApiErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/payments")
                .build();
    }

    public DepositHoldResponseDto holdDeposit(int amount, String memberPublicId, Long auctionId) {
        DepositHoldRequestDto request = new DepositHoldRequestDto(amount, memberPublicId, auctionId);
        SuccessResponseDto<DepositHoldResponseDto> response = restClient.post()
                .uri("/deposits/hold")
                .contentType(MediaType.APPLICATION_JSON)
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
}
