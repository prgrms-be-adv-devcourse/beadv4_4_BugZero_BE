package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;

@ExtendWith(MockitoExtension.class)
class AuctionFacadeTest {

	@InjectMocks
	private AuctionFacade auctionFacade;

	@Mock
	private AuctionCreateBidUseCase auctionCreateBidUseCase;

	@Mock
	private AuctionReadUseCase auctionReadUseCase;

	@Test
	@DisplayName("입찰 생성 요청 시 UseCase를 호출하고 결과를 반환한다")
	void createBid_Success() {
		// given
		Long auctionId = 1L;
		Long memberId = 100L;
		String memberPublicId = "user_uuid";
		int bidAmount = 50000;

		BidResponseDto bidResponse = new BidResponseDto(
			1L, auctionId, "public-id", LocalDateTime.now(), (long) bidAmount, (long) bidAmount
		);

		given(auctionCreateBidUseCase.createBid(auctionId, memberPublicId, bidAmount))
			.willReturn(bidResponse);

		// when
		SuccessResponseDto<BidResponseDto> result = auctionFacade.createBid(auctionId, memberPublicId, bidAmount);

		// then
		assertThat(result.status()).isEqualTo(SuccessType.CREATED.getHttpStatus());
		assertThat(result.message()).isEqualTo(SuccessType.CREATED.getMessage());
		assertThat(result.data()).isEqualTo(bidResponse);

		verify(auctionCreateBidUseCase).createBid(auctionId, memberPublicId, bidAmount);
	}

	@Test
	@DisplayName("경매 입찰 기록 조회: ReadUseCase에 위임한다")
	void getBidLogs_Success() {
		// given
		Long auctionId = 1L;
		Pageable pageable = PageRequest.of(0, 10);

		BidLogResponseDto logDto = new BidLogResponseDto(1L, "user_masked", LocalDateTime.now(), 50000);
		PagedResponseDto<BidLogResponseDto> expectedResponse = new PagedResponseDto<>(
			List.of(logDto), new PageDto(1, 10, 1, 1, false, false)
		);

		given(auctionReadUseCase.getBidLogs(eq(auctionId), any(Pageable.class)))
			.willReturn(expectedResponse);

		// when
		PagedResponseDto<BidLogResponseDto> result = auctionFacade.getBidLogs(auctionId, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).publicId()).isEqualTo("user_masked");
		assertThat(result.data().get(0).bidAmount()).isEqualTo(50000);

		verify(auctionReadUseCase).getBidLogs(eq(auctionId), any(Pageable.class));
	}

	@Test
	@DisplayName("나의 입찰 내역 조회: ReadUseCase에 위임한다")
	void getMyBids_Success() {
		// given
		Long memberId = 100L;
		String memberPublicId = "user_uuid";
		Pageable pageable = PageRequest.of(0, 10);

		MyBidResponseDto myBidDto = new MyBidResponseDto(
			1L, 10L, 50L, 15000, LocalDateTime.now(), AuctionStatus.IN_PROGRESS, 15000, LocalDateTime.now().plusDays(1)
		);
		PagedResponseDto<MyBidResponseDto> expectedResponse = new PagedResponseDto<>(
			List.of(myBidDto), new PageDto(1, 10, 1, 1, false, false)
		);

		given(auctionReadUseCase.getMyBids(eq(memberPublicId), eq(null), any(Pageable.class)))
			.willReturn(expectedResponse);

		// when
		PagedResponseDto<MyBidResponseDto> result = auctionFacade.getMyBids(memberPublicId, null, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		MyBidResponseDto dto = result.data().get(0);
		assertThat(dto.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
		assertThat(dto.bidAmount()).isEqualTo(15000);
		assertThat(dto.currentPrice()).isEqualTo(15000);

		verify(auctionReadUseCase).getMyBids(eq(memberPublicId), eq(null), any(Pageable.class));
	}

	@Test
	@DisplayName("나의 판매 목록 조회: ReadUseCase에 위임한다")
	void getMySales_Success() {
		// given
		Long sellerId = 1L;
		String sellerPublicId = "seller_uuid";
		AuctionFilterType filter = AuctionFilterType.ALL;
		Pageable pageable = PageRequest.of(0, 10);

		MySaleResponseDto saleDto1 = MySaleResponseDto.builder()
			.auctionId(100L)
			.title("Product 1")
			.currentPrice(1000)
			.bidCount(5)
			.tradeStatus(AuctionOrderStatus.PROCESSING)
			.build();

		MySaleResponseDto saleDto2 = MySaleResponseDto.builder()
			.auctionId(200L)
			.title("Product 2")
			.currentPrice(2000)
			.bidCount(0)
			.tradeStatus(null)
			.build();

		PagedResponseDto<MySaleResponseDto> expectedResponse = new PagedResponseDto<>(
			List.of(saleDto1, saleDto2), new PageDto(1, 10, 2, 1, false, false)
		);

		given(auctionReadUseCase.getMySales(eq(sellerPublicId), eq(filter), any(Pageable.class)))
			.willReturn(expectedResponse);

		// when
		PagedResponseDto<MySaleResponseDto> result = auctionFacade.getMySales(sellerPublicId, filter, pageable);

		// then
		assertThat(result.data()).hasSize(2);

		MySaleResponseDto dto1 = result.data().stream()
			.filter(d -> d.auctionId().equals(100L)).findFirst().orElseThrow();
		assertThat(dto1.title()).isEqualTo("Product 1");
		assertThat(dto1.bidCount()).isEqualTo(5);
		assertThat(dto1.tradeStatus()).isEqualTo(AuctionOrderStatus.PROCESSING);

		MySaleResponseDto dto2 = result.data().stream()
			.filter(d -> d.auctionId().equals(200L)).findFirst().orElseThrow();
		assertThat(dto2.title()).isEqualTo("Product 2");
		assertThat(dto2.bidCount()).isEqualTo(0);
		assertThat(dto2.tradeStatus()).isNull();

		verify(auctionReadUseCase).getMySales(eq(sellerPublicId), eq(filter), any(Pageable.class));
	}
}