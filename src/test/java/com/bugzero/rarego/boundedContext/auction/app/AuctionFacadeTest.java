package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;

import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;

@ExtendWith(MockitoExtension.class)
class AuctionFacadeTest {

	@InjectMocks
	private AuctionFacade auctionFacade;

	@Mock
	private BidRepository bidRepository;

	@Mock
	private AuctionCreateBidUseCase auctionCreateBidUseCase;

	@Test
	@DisplayName("경매 입찰 기록 조회: DTO 변환 및 PublicId 확인")
	void getBidLogs_Success() {
		// given
		Long auctionId = 1L;
		Pageable pageable = PageRequest.of(0, 10);

		AuctionMember bidder = AuctionMember.builder().publicId("user_masked_id").build();

		// [수정] Bid 생성 시 Auction 객체 필수 (연관관계 null 방지)
		Auction auction = Auction.builder().startPrice(1000).build();

		Bid bid = Bid.builder()
			.auction(auction) // [추가] Auction 더미 객체 주입
			.bidder(bidder)
			.bidAmount(50000)
			.build();

		Page<Bid> bidPage = new PageImpl<>(List.of(bid));
		given(bidRepository.findAllByAuctionIdOrderByBidTimeDesc(eq(auctionId), any(Pageable.class)))
			.willReturn(bidPage);

		// when
		PagedResponseDto<BidLogResponseDto> result = auctionFacade.getBidLogs(auctionId, pageable);

		// then
		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().get(0).publicId()).isEqualTo("user_masked_id");
		assertThat(result.getItems().get(0).bidAmount()).isEqualTo(50000);
	}

	@Test
	@DisplayName("나의 입찰 내역 조회: 상품명 및 경매 상태 매핑 확인")
	void getMyBids_Success() {
		// given
		Long memberId = 100L;
		Pageable pageable = PageRequest.of(0, 10);

		Product product = Product.builder().id(10L).name("레고 스타워즈").build();
		Auction auction = Auction.builder()
			.product(product)
			.startPrice(10000)
			.tickSize(1000)
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(1))
			.build();
		auction.startAuction(); // IN_PROGRESS

		Bid bid = Bid.builder()
			.auction(auction)
			.bidder(AuctionMember.builder().id(memberId).build())
			.bidAmount(15000)
			.build();

		Page<Bid> bidPage = new PageImpl<>(List.of(bid));

		// Repository 호출 Stubbing
		given(bidRepository.findMyBids(eq(memberId), any(), any(Pageable.class)))
			.willReturn(bidPage);

		// when
		PagedResponseDto<MyBidResponseDto> result = auctionFacade.getMyBids(memberId, null, pageable);

		// then
		assertThat(result.getItems()).hasSize(1);
		MyBidResponseDto dto = result.getItems().get(0);

		assertThat(dto.productName()).isEqualTo("레고 스타워즈");
		assertThat(dto.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
		assertThat(dto.bidAmount()).isEqualTo(15000);
	}
}