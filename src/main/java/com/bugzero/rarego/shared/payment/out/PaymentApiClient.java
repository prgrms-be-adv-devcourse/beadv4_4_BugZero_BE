package com.bugzero.rarego.shared.payment.out;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.shared.payment.dto.DepositHoldRequest;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponse;

@Service
public class PaymentApiClient {
    private final RestClient restClient;

    public PaymentApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/payments")
                .build();
    }

    public DepositHoldResponse holdDeposit(int amount, Long memberId, Long auctionId) {
        DepositHoldRequest request = new DepositHoldRequest(amount, memberId, auctionId);
        return restClient.post()
                .uri("/deposits/hold")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
