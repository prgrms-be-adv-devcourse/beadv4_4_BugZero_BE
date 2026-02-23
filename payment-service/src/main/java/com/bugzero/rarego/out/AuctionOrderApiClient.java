package com.bugzero.rarego.out;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.exception.InternalApiErrorHandler;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.security.SystemAuthTokenProvider;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;

@Service
public class AuctionOrderApiClient {
	private static final String HAS_NEXT_HEADER = "X-Has-Next";

	private final RestClient restClient;
	private final InternalApiErrorHandler errorHandler;

	public AuctionOrderApiClient(
		@Value("${custom.global.internalBackUrl}") String internalBackUrl,
		InternalApiErrorHandler errorHandler,
		SystemAuthTokenProvider systemAuthTokenProvider
	) {
		this.errorHandler = errorHandler;
		this.restClient = RestClient.builder()
			.baseUrl(internalBackUrl + "/api/v1/internal/auctions")
			.requestInterceptor((request, body, execution) -> {
				request.getHeaders().setBearerAuth(systemAuthTokenProvider.getSystemAccessToken());
				return execution.execute(request, body);
			})
			.build();
	}

	public AuctionOrderDto getOrder(Long auctionId) {
		SuccessResponseDto<AuctionOrderDto> response = restClient.get()
			.uri("/orders/{auctionId}", auctionId)
			.retrieve()
			.onStatus(HttpStatusCode::isError, errorHandler::handle)
			.body(new ParameterizedTypeReference<>() {
			});

		if (response == null || response.data() == null) {
			throw new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND);
		}
		return response.data();
	}

	public void completeOrder(Long auctionId) {
		restClient.post()
			.uri("/orders/{auctionId}/complete", auctionId)
			.retrieve()
			.onStatus(HttpStatusCode::isError, errorHandler::handle)
			.toBodilessEntity();
	}

	public void failOrder(Long auctionId) {
		restClient.post()
			.uri("/orders/{auctionId}/fail", auctionId)
			.retrieve()
			.onStatus(HttpStatusCode::isError, errorHandler::handle)
			.toBodilessEntity();
	}

	public AuctionOrderDto refundOrder(Long auctionId) {
		SuccessResponseDto<AuctionOrderDto> response = restClient.post()
			.uri("/orders/{auctionId}/refund", auctionId)
			.retrieve()
			.onStatus(HttpStatusCode::isError, errorHandler::handle)
			.body(new ParameterizedTypeReference<>() {
			});

		if (response == null || response.data() == null) {
			throw new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND);
		}
		return response.data();
	}

	public AuctionOrderSlice findTimeoutOrders(LocalDateTime deadline, Pageable pageable) {
		ResponseEntity<SuccessResponseDto<List<AuctionOrderDto>>> entity = restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/orders/timeout")
				.queryParam("deadline", deadline)
				.queryParam("page", pageable.getPageNumber())
				.queryParam("size", pageable.getPageSize())
				.build())
			.retrieve()
			.onStatus(HttpStatusCode::isError, errorHandler::handle)
			.toEntity(new ParameterizedTypeReference<>() {
			});

		return getAuctionOrderSlice(entity);
	}

	public AuctionOrderSlice findExpiringSoonOrders(LocalDateTime targetEndedAt, Pageable pageable) {
		ResponseEntity<SuccessResponseDto<List<AuctionOrderDto>>> entity = restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/orders/expiring-soon")
				.queryParam("targetEndedAt", targetEndedAt)
				.queryParam("page", pageable.getPageNumber())
				.queryParam("size", pageable.getPageSize())
				.build())
			.retrieve()
			.onStatus(HttpStatusCode::isError, errorHandler::handle)
			.toEntity(new ParameterizedTypeReference<>() {
			});

		return getAuctionOrderSlice(entity);
	}

	public void markAsNoticed(Long orderId) {
		restClient.patch()
			.uri(uriBuilder -> uriBuilder
				.path("/orders/{orderId}/notice")
				.build(orderId))
			.retrieve()
			.onStatus(HttpStatusCode::isError, errorHandler::handle)
			.toBodilessEntity();
	}

	private AuctionOrderApiClient.AuctionOrderSlice getAuctionOrderSlice(
		ResponseEntity<SuccessResponseDto<List<AuctionOrderDto>>> entity) {
		SuccessResponseDto<List<AuctionOrderDto>> body = entity.getBody();
		List<AuctionOrderDto> content = body == null || body.data() == null ? List.of() : body.data();
		boolean hasNext = Boolean.parseBoolean(entity.getHeaders().getFirst(HAS_NEXT_HEADER));

		return new AuctionOrderSlice(content, hasNext);
	}

	public record AuctionOrderSlice(List<AuctionOrderDto> content, boolean hasNext) {
	}
}
