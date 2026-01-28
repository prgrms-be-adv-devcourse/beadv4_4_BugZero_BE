package com.bugzero.rarego.boundedContext.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionTest {

	@Test
	@DisplayName("경매 마감 3분 전 연장 테스트 - 5회 제한 확인")
	void extendEndTimeTest() {
		// Given: 마감 2분 남은 경매 생성
		LocalDateTime initialNow = LocalDateTime.now();
		LocalDateTime startTime = initialNow.minusMinutes(58);
		LocalDateTime endTime = initialNow.plusMinutes(2); // 2분 남음

		Auction auction = Auction.builder()
			.startPrice(1000)
			.startTime(startTime)
			.endTime(endTime)
			.durationDays(1)
			.build();

		// 테스트를 위해 extensionCount 초기화 (필요시 Setter 사용, 없으면 무시)
		auction.setExtensionCountForTest(0);

		// When & Then

		// 1회~5회 연장 시도: 모두 성공해야 함
		for (int i = 1; i <= 5; i++) {
			// [수정] 중요: 연장이 되어 종료 시간이 늘어났으므로,
			// 테스트 시점(now)을 '변경된 종료 시간의 1분 전'으로 가정하여 호출해야 함
			LocalDateTime assumeNow = auction.getEndTime().minusMinutes(1);

			boolean isExtended = auction.extendEndTimeIfClose(assumeNow);

			assertThat(isExtended).as(i + "번째 연장 시도 실패").isTrue();
			assertThat(auction.getExtensionCount()).isEqualTo(i);
		}

		// 6회째 시도: 횟수 제한으로 실패해야 함
		// 역시 마감 임박한 시간으로 가정하고 호출
		LocalDateTime assumeNowLast = auction.getEndTime().minusMinutes(1);
		boolean isExtended6 = auction.extendEndTimeIfClose(assumeNowLast);

		assertThat(isExtended6).as("6번째는 연장되면 안됨").isFalse();
		assertThat(auction.getExtensionCount()).isEqualTo(5); // 여전히 5여야 함
	}
}