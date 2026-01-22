package com.bugzero.rarego.shared.payment.out;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentApiClient {
    private final RestClient restClient;

    public PaymentApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/payments")
                .build();
    }

    public DepositHoldResponseDto holdDeposit(int amount, String memberPublicId, Long auctionId) {
        DepositHoldRequestDto request = new DepositHoldRequestDto(amount, memberPublicId, auctionId);
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
}
