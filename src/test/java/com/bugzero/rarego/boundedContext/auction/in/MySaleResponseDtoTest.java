package com.bugzero.rarego.boundedContext.auction.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

class MySaleResponseDtoTest {

	@Test
	@DisplayName("경매가 진행 중(IN_PROGRESS)이면 입찰이 없어도 조치 필요(false)가 아니다")
	void actionRequired_false_when_in_progress() {
		// given
		Auction auction = createAuction(AuctionStatus.IN_PROGRESS);
		AuctionOrder order = null;
		int bidCount = 0; // 입찰 없음

		// when
		MySaleResponseDto dto = MySaleResponseDto.from(auction, null, order, bidCount);

		// then
		assertThat(dto.actionRequired()).isFalse();
	}

	@Test
	@DisplayName("경매가 종료(ENDED)되었고 입찰자가 0명이면 조치 필요(true)이다 - 유찰")
	void actionRequired_true_when_ended_and_no_bids() {
		// given
		Auction auction = createAuction(AuctionStatus.ENDED);
		AuctionOrder order = null;
		int bidCount = 0; // 입찰 0명

		// when
		MySaleResponseDto dto = MySaleResponseDto.from(auction, null, order, bidCount);

		// then
		assertThat(dto.actionRequired()).isTrue();
	}

	@Test
	@DisplayName("경매가 종료되었고 낙찰되었으나(주문 생성), 결제 실패(FAILED)라면 조치 필요(true)이다")
	void actionRequired_true_when_payment_failed() {
		// given
		Auction auction = createAuction(AuctionStatus.ENDED);
		AuctionOrder order = mock(AuctionOrder.class);
		when(order.getStatus()).thenReturn(AuctionOrderStatus.FAILED); // 결제 실패 상태

		int bidCount = 5; // 입찰은 있었음

		// when
		MySaleResponseDto dto = MySaleResponseDto.from(auction, null, order, bidCount);

		// then
		assertThat(dto.actionRequired()).isTrue();
	}

	@Test
	@DisplayName("경매가 종료되었고 결제 성공(SUCCESS)했다면 조치 필요(false)가 아니다")
	void actionRequired_false_when_payment_success() {
		// given
		Auction auction = createAuction(AuctionStatus.ENDED);
		AuctionOrder order = mock(AuctionOrder.class);
		when(order.getStatus()).thenReturn(AuctionOrderStatus.SUCCESS); // 결제 성공

		int bidCount = 3;

		// when
		MySaleResponseDto dto = MySaleResponseDto.from(auction, null, order, bidCount);

		// then
		assertThat(dto.actionRequired()).isFalse();
	}

	@Test
	@DisplayName("경매가 종료되었고 결제 진행 중(PROCESSING)이라면 조치 필요(false)가 아니다")
	void actionRequired_false_when_payment_processing() {
		// given
		Auction auction = createAuction(AuctionStatus.ENDED);
		AuctionOrder order = mock(AuctionOrder.class);
		when(order.getStatus()).thenReturn(AuctionOrderStatus.PROCESSING); // 결제 중

		int bidCount = 1;

		// when
		MySaleResponseDto dto = MySaleResponseDto.from(auction, null, order, bidCount);

		// then
		assertThat(dto.actionRequired()).isFalse();
	}

	// --- Helper Method ---
	private Auction createAuction(AuctionStatus status) {
		// Auction 객체 생성 (Builder 사용 가정)
		// 테스트에 필요한 status와 endTime만 설정
		Auction auction = Auction.builder()
			.productId(1L)
			.sellerId(1L)
			.startPrice(1000)
			.durationDays(3)
			.startTime(LocalDateTime.now().minusDays(3))
			.endTime(LocalDateTime.now().minusHours(1)) // 이미 종료된 시간
			.build();

		ReflectionTestUtils.setField(auction, "status", status);

		return auction;
	}
}