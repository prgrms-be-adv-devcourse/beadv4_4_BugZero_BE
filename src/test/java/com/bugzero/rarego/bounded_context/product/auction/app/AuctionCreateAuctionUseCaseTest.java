package com.bugzero.rarego.bounded_context.product.auction.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.bounded_context.auction.domain.Auction;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionMember;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionStatus;
import com.bugzero.rarego.bounded_context.auction.out.AuctionRepository;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;

@ExtendWith(MockitoExtension.class)
class AuctionCreateAuctionUseCaseTest {
	@Mock
	private AuctionRepository auctionRepository;

	@Mock
	private ProductAuctionSupport productAuctionSupport;

	@InjectMocks
	private AuctionCreateAuctionUseCase auctionCreateAuctionUseCase;

	@Captor
	private ArgumentCaptor<Auction> auctionCaptor;

	private final String PUBLIC_ID = "seller-uuid";

	@Test
	@DisplayName("상품 ID와 기간을 입력받아 경매 정보를 정상적으로 생성한다")
	void createAuction_Success() {
		// given
		Long productId = 1L;
		int durationDays = 24;
		ProductAuctionRequestDto requestDto = ProductAuctionRequestDto.builder()
			.startPrice(10000)
			.durationDays(durationDays)
			.build();
		AuctionMember commonSeller = AuctionMember.builder()
			.id(1L)
			.publicId(PUBLIC_ID)
			.build();

		// Mocking: 저장 시 ID가 100인 객체가 반환된다고 가정
		Auction mockAuction = requestDto.toEntity(productId, 1L);
		ReflectionTestUtils.setField(mockAuction, "id", 100L);
		when(auctionRepository.save(any(Auction.class))).thenReturn(mockAuction);
		given(productAuctionSupport.getAuctionMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productAuctionSupport.determineTickSize(requestDto.startPrice())).willReturn(1000);



		// when
		Long savedAuctionId = auctionCreateAuctionUseCase.createAuction(productId, PUBLIC_ID, requestDto);

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