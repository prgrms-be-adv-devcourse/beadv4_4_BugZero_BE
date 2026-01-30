package com.bugzero.rarego.bounded_context.product.auction.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.bounded_context.auction.domain.Auction;
import com.bugzero.rarego.bounded_context.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class AuctionDetermineStartAuctionUseCaseTest {
	@Mock
	AuctionRepository auctionRepository;

	@InjectMocks
	AuctionDetermineStartAuctionUseCase useCase;

	private final Long AUCTION_ID = 1L;
	private final int durationDays = 2;

	@Test
	@DisplayName("성공: 시작 시간이 없는 경매의 시작 시간을 확정하면 24시간 뒤 정각으로 설정된다")
	void determineStartAuction_success() {
		// given
		// 시작 시간이 설정되지 않은(null) 경매 객체 생성
		Auction auction = Auction.builder()
			.startTime(null)
			.durationDays(durationDays)
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);

		Auction spyAuction = spy(auction);
		given(auctionRepository.findByIdAndDeletedIsFalse(AUCTION_ID))
			.willReturn(Optional.of(spyAuction));

		// when
		Long resultId = useCase.determineStartAuction(AUCTION_ID);

		// then
		assertThat(resultId).isEqualTo(AUCTION_ID);

		// 캡처를 통해 실제 계산된 시간을 검증
		ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(spyAuction).determineStart(timeCaptor.capture());

		LocalDateTime capturedTime = timeCaptor.getValue();

		// 시간 검증: 분/초/나노초가 모두 0인지 확인
		assertAll(
			() -> assertThat(capturedTime.getMinute()).isZero(),
			() -> assertThat(capturedTime.getSecond()).isZero(),
			() -> assertThat(capturedTime.getNano()).isZero(),
			() -> assertThat(capturedTime).isAfter(LocalDateTime.now().plusHours(23))
		);
	}

	@Test
	@DisplayName("실패: 이미 시작 시간이 정해진 경매라면 예외가 발생한다")
	void determineStartAuction_fail_already_has_time() {
		// given
		Auction auctionWithTime = Auction.builder()
			.startTime(LocalDateTime.now())
			.durationDays(durationDays)
			.build();

		given(auctionRepository.findByIdAndDeletedIsFalse(AUCTION_ID))
			.willReturn(Optional.of(auctionWithTime));

		// when & then
		assertThatThrownBy(() -> useCase.determineStartAuction(AUCTION_ID))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_ALREADY_HAS_START_TIME);
	}

	@Test
	@DisplayName("실패: 경매를 찾을 수 없으면 예외가 발생한다")
	void determineStartAuction_fail_not_found() {
		// given
		given(auctionRepository.findByIdAndDeletedIsFalse(anyLong()))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> useCase.determineStartAuction(AUCTION_ID))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_NOT_FOUND);
	}




}