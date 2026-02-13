package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class AuctionSettleOneUseCaseTest {

	@Mock
	private AuctionSettlementSupport support;

	@InjectMocks
	private AuctionSettleOneUseCase useCase;

	@Test
	@DisplayName("검증을 모두 통과하면 Support를 통해 정산 처리를 수행한다")
	void execute_Success() {
		// given
		Long auctionId = 1L;

		// when
		useCase.execute(auctionId);

		// then
		verify(support, times(1)).processSettlement(auctionId);
	}

	@Test
	@DisplayName("이미 종료된(ENDED) 경매는 아무런 처리를 하지 않고 리턴한다")
	void execute_AlreadyEnded() {
		// given
		Long auctionId = 1L;
		doThrow(new CustomException(ErrorType.AUCTION_NOT_FOUND_OR_ALREADY_SETTLED))
			.when(support).processSettlement(auctionId);

		// when & then
		assertThatCode(() -> useCase.execute(auctionId)).doesNotThrowAnyException();
		verify(support).processSettlement(auctionId);
	}

	@Test
	@DisplayName("경매가 존재하지 않으면 예외 발생")
	void execute_AuctionNotFound() {
		// given
		Long auctionId = 1L;
		doThrow(new CustomException(ErrorType.AUCTION_NOT_FOUND))
			.when(support).processSettlement(auctionId);

		// when & then
		assertThatThrownBy(() -> useCase.execute(auctionId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_NOT_FOUND);
	}

	@Test
	@DisplayName("진행 중(IN_PROGRESS)이 아닌 경매는 예외 발생")
	void execute_NotInProgress() {
		// given
		Long auctionId = 1L;
		doThrow(new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS))
			.when(support).processSettlement(auctionId);

		// when & then
		assertThatThrownBy(() -> useCase.execute(auctionId))
			.isInstanceOf(CustomException.class);
	}
}
