package com.bugzero.rarego.shared.auction.out;

import java.util.Optional;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;
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
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

@Service
public class AuctionApiClient {
    private final RestClient restClient;
    private final InternalApiErrorHandler errorHandler;

    public AuctionApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl, InternalApiErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/auctions")
                .build();
    }

    public Long createAuction(Long productId, String publicId, ProductAuctionRequestDto productAuctionRequestDto) {
        SuccessResponseDto<Long> response = restClient.post()
                .uri("/{productId}/{publicId}", productId, publicId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(productAuctionRequestDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                    (httpRequest, httpResponse) -> errorHandler.handleWithDefault(httpRequest, httpResponse, ErrorType.AUCTION_CREATE_FAILED))
                .body(new ParameterizedTypeReference<>() {
                });

        return Optional.ofNullable(response)
                .map(SuccessResponseDto::data)
                .orElseThrow(() -> new CustomException(ErrorType.AUCTION_CREATE_FAILED));
    }

    public Long updateAuction(String publicId, ProductAuctionUpdateDto productAuctionUpdateDto) {
        SuccessResponseDto<Long> response = restClient.patch()
            .uri("/{publicId}", publicId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(productAuctionUpdateDto)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse)
                -> errorHandler.handleWithDefault(httpRequest, httpResponse, ErrorType.AUCTION_UPDATE_FAILED))
            .body(new ParameterizedTypeReference<>() {
            });

        return Optional.ofNullable(response)
            .map(SuccessResponseDto::data)
            .orElseThrow(() -> new CustomException(ErrorType.AUCTION_UPDATE_FAILED));
    }

    /**
     * 진행 중인 입찰이 있는지 확인
     * 종료되지 않은 경매에 입찰이 있으면 true
     */
    public boolean hasActiveBids(String publicId) {
        SuccessResponseDto<Boolean> response = restClient.get()
                .uri("/members/{publicId}/bids/active", publicId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                    throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        return response != null && Boolean.TRUE.equals(response.data());
    }

    /**
     * 진행 중인 판매가 있는지 확인
     * 검수/경매가 완료되지 않은 상품이 있으면 true
     */
    public boolean hasActiveSales(String publicId) {
        SuccessResponseDto<Boolean> response = restClient.get()
                .uri("/members/{publicId}/sales/active", publicId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                    throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
                })
                .body(new ParameterizedTypeReference<>() {
                });

        return response != null && Boolean.TRUE.equals(response.data());
    }

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

    public void deleteAuction(String publicId,Long productId) {
        restClient.delete()
            .uri("/{productId}/{publicId}", productId ,publicId)
            .retrieve()
            .onStatus(HttpStatusCode::isError,
                (httpRequest, httpResponse) -> errorHandler.handleWithDefault(httpRequest, httpResponse, ErrorType.AUCTION_DELETE_FAILED))
            .toBodilessEntity();
    }
}
