package com.bugzero.rarego.in;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bugzero.rarego.app.AuctionFacade;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 경매 실시간 입찰 스트림 API
 */
@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Slf4j
public class AuctionBidStreamController {

	private final AuctionFacade auctionFacade;

	@Operation(summary = "경매 실시간 입찰 이벤트 구독", description = "경매 실시간 입찰 이벤트를 구독합니다.")
	@GetMapping(value = "/{auctionId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@PathVariable Long auctionId) {
		log.info("경매 {} 스트림 구독 요청", auctionId);
		return auctionFacade.subscribeAuctionStream(auctionId);
	}

	@Operation(summary = "전체 구독자 수 조회", description = "전체 구독자 수를 조회합니다. (모니터링용)")
	@GetMapping("/subscribers/count")
	public int getTotalSubscribers() {
		return auctionFacade.getTotalSubscribers();
	}

	@Operation(summary = "특정 경매 구독자 수 조회", description = "특정 경매 구독자 수를 조회합니다. (모니터링용)")
	@GetMapping("/{auctionId}/subscribers/count")
	public int getAuctionSubscribers(@PathVariable Long auctionId) {
		return auctionFacade.getAuctionSubscribers(auctionId);
	}
}
