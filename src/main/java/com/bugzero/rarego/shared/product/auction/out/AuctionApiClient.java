package com.bugzero.rarego.shared.product.auction.out;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;

@Service
public class AuctionApiClient {
	private final RestClient restClient;

	public AuctionApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl ) {
		this.restClient = RestClient.builder()
			.baseUrl(internalBackUrl + "/api/v1/products")
			.build();
	}

	public Long createAuction(Long productId, String sellerUUID, ProductAuctionRequestDto productAuctionRequestDto) {
		SuccessResponseDto<Long> response = restClient.post()
			.uri("/{productId}/auctions/{sellerUUID}", productId, sellerUUID)
			.body(productAuctionRequestDto)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
				throw new CustomException(ErrorType.AUCTION_CREATE_FAILED);
			})
			.body(new ParameterizedTypeReference<>() {});

		Long result = (response != null) ? response.data() : null;

		//result값이 null 인경우 경매정보 생성 실패 인식
		return Optional.ofNullable(result)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_CREATE_FAILED));
	}
}
