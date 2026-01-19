package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
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
	private AuctionMemberRepository auctionMemberRepository; // ID 조회를 위해 필요
	@Mock
	private AuctionRepository auctionRepository; // ID 조회를 위해 필요
	@Mock
	private AuctionCreateBidUseCase auctionCreateBidUseCase;

	@Test
	@DisplayName("경매 입찰 기록 조회: 닉네임 매핑 확인")
	void getBidLogs_Success() {
		// given
		Long auctionId = 1L;
		Long bidderId = 100L;
		Pageable pageable = PageRequest.of(0, 10);

		// Bid 데이터 (ID만 가짐)
		Bid bid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(bidderId)
			.bidAmount(50000)
			.build();
		ReflectionTestUtils.setField(bid, "id", 1L);

		// Member 데이터 (닉네임용)
		AuctionMember bidder = AuctionMember.builder().publicId("user_masked").build();
		ReflectionTestUtils.setField(bidder, "id", bidderId);

		// Mocking
		given(bidRepository.findAllByAuctionIdOrderByBidTimeDesc(eq(auctionId), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(bid)));

		// Facade가 ID로 Member를 조회함
		given(auctionMemberRepository.findAllById(anySet())).willReturn(List.of(bidder));

		// when
		PagedResponseDto<BidLogResponseDto> result = auctionFacade.getBidLogs(auctionId, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).publicId()).isEqualTo("user_masked");
		assertThat(result.data().get(0).bidAmount()).isEqualTo(50000);
	}

	@Test
	@DisplayName("나의 입찰 내역 조회: Auction 정보 매핑 확인 (productName 제외)")
	void getMyBids_Success() {
		// given
		Long memberId = 100L;
		Long auctionId = 10L;
		Pageable pageable = PageRequest.of(0, 10);

		// Auction 데이터
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(10000)
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(1))
			.durationDays(1)
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);

		auction.startAuction(); // 상태: IN_PROGRESS

		// [추가] currentPrice가 null이면 DTO 변환 시 NPE 발생하므로 값 설정 필수!
		auction.updateCurrentPrice(15000);

		// Bid 데이터
		Bid bid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(memberId)
			.bidAmount(15000)
			.build();
		ReflectionTestUtils.setField(bid, "id", 1L);

		// Mocking
		given(bidRepository.findMyBids(eq(memberId), any(), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(bid)));

		// Facade가 ID로 Auction을 조회함
		given(auctionRepository.findAllById(anySet())).willReturn(List.of(auction));

		// when
		PagedResponseDto<MyBidResponseDto> result = auctionFacade.getMyBids(memberId, null, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		MyBidResponseDto dto = result.data().get(0);

		assertThat(dto.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
		assertThat(dto.bidAmount()).isEqualTo(15000);

		// (선택) 현재가 확인
		assertThat(dto.currentPrice()).isEqualTo(15000);
	}
}