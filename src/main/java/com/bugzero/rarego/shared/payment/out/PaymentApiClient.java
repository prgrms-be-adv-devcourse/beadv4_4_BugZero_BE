package com.bugzero.rarego.shared.payment.out;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

@Service
public class PaymentApiClient {
    private final RestClient restClient;

    public PaymentApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/payments")
                .build();
    }

    public DepositHoldResponseDto holdDeposit(int amount, Long memberId, Long auctionId) {
        DepositHoldRequestDto request = new DepositHoldRequestDto(amount, memberId, auctionId);
        SuccessResponseDto<DepositHoldResponseDto> response = restClient.post()
                .uri("/deposits/hold")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null || response.data() == null) {
            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
        }
        return response.data();
    }
}
