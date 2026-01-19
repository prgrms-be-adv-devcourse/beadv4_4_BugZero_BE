package com.bugzero.rarego.boundedContext.product.auction.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;

@ExtendWith(MockitoExtension.class)
class AuctionCreateAuctionUseCaseTest {
	@Mock
	private AuctionRepository auctionRepository;

	@InjectMocks
	private AuctionCreateAuctionUseCase auctionCreateAuctionUseCase;

	@Captor
	private ArgumentCaptor<Auction> auctionCaptor;

	@Test
	@DisplayName("상품 ID와 기간을 입력받아 경매 정보를 정상적으로 생성한다")
	void createAuction_Success() {
		// given
		Long productId = 1L;
		int durationDays = 24;
		String sellerUUID = "1L";
		ProductAuctionRequestDto requestDto = ProductAuctionRequestDto.builder()
			.startPrice(10000)
			.durationDays(durationDays)
			.build();

		// Mocking: 저장 시 ID가 100인 객체가 반환된다고 가정
		Auction mockAuction = requestDto.toEntity(productId, 1L, 1000);
		ReflectionTestUtils.setField(mockAuction, "id", 100L);
		when(auctionRepository.save(any(Auction.class))).thenReturn(mockAuction);

		// when
		Long savedAuctionId = auctionCreateAuctionUseCase.createAuction(productId, sellerUUID, requestDto);

		verify(auctionRepository).save(auctionCaptor.capture());
		Auction capturedAuction = auctionCaptor.getValue();

		// then
		assertThat(capturedAuction.getProductId()).isEqualTo(productId);
		assertThat(capturedAuction.getStartPrice()).isEqualTo(10000);
		assertThat(capturedAuction.getDurationDays()).isEqualTo(durationDays);
		assertThat(capturedAuction.getTickSize()).isEqualTo(1000);
		assertThat(capturedAuction.getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
		assertThat(savedAuctionId).isEqualTo(100L);
		verify(auctionRepository, times(1)).save(any(Auction.class));
	}
}