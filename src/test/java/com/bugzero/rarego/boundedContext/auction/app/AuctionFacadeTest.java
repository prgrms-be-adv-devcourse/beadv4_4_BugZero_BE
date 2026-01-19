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
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
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
	private BidRepository bidRepository;
	@Mock
	private AuctionMemberRepository auctionMemberRepository;
	@Mock
	private AuctionRepository auctionRepository;
	@Mock
	private ProductRepository productRepository;
	@Mock
	private AuctionOrderRepository auctionOrderRepository;

	@Test
	@DisplayName("입찰 생성 요청 시 UseCase를 호출하고 결과를 반환한다")
	void createBid_Success() {
		// given
		Long auctionId = 1L;
		Long memberId = 100L;
		int bidAmount = 50000;

		// [수정] Builder 대신 생성자 사용 (Record)
		BidResponseDto bidResponse = new BidResponseDto(
			1L, auctionId, "public-id", LocalDateTime.now(), (long) bidAmount, (long) bidAmount
		);

		SuccessResponseDto<BidResponseDto> expectedResponse = SuccessResponseDto.from(
			SuccessType.CREATED,
			bidResponse
		);

		given(auctionCreateBidUseCase.createBid(auctionId, memberId, bidAmount))
			.willReturn(expectedResponse);

		// when
		SuccessResponseDto<BidResponseDto> result = auctionFacade.createBid(auctionId, memberId, bidAmount);

		// then
		assertThat(result).isEqualTo(expectedResponse);
		verify(auctionCreateBidUseCase).createBid(auctionId, memberId, bidAmount);
	}

	@Test
	@DisplayName("경매 입찰 기록 조회: 입찰자와 매핑하여 반환한다")
	void getBidLogs_Success() {
		// given
		Long auctionId = 1L;
		Long bidderId = 100L;
		Pageable pageable = PageRequest.of(0, 10);

		Bid bid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(bidderId)
			.bidAmount(50000)
			.build();
		ReflectionTestUtils.setField(bid, "id", 1L);
		// BaseIdAndTime 필드 세팅 (필요 시)
		// ReflectionTestUtils.setField(bid, "createdAt", LocalDateTime.now());

		AuctionMember bidder = AuctionMember.builder().publicId("user_masked").build();
		ReflectionTestUtils.setField(bidder, "id", bidderId);

		given(bidRepository.findAllByAuctionIdOrderByBidTimeDesc(eq(auctionId), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(bid)));

		given(auctionMemberRepository.findAllById(anySet())).willReturn(List.of(bidder));

		// when
		PagedResponseDto<BidLogResponseDto> result = auctionFacade.getBidLogs(auctionId, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).publicId()).isEqualTo("user_masked");
		assertThat(result.data().get(0).bidAmount()).isEqualTo(50000);
	}

	@Test
	@DisplayName("나의 입찰 내역 조회: 경매 정보와 매핑하여 반환한다")
	void getMyBids_Success() {
		// given
		Long memberId = 100L;
		Long auctionId = 10L;
		Pageable pageable = PageRequest.of(0, 10);

		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(10000)
			.tickSize(1000)
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(1))
			.durationDays(1)
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);
		auction.startAuction();
		auction.updateCurrentPrice(15000);

		Bid bid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(memberId)
			.bidAmount(15000)
			.build();
		ReflectionTestUtils.setField(bid, "id", 1L);

		given(bidRepository.findMyBids(eq(memberId), eq(null), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(bid)));

		given(auctionRepository.findAllById(anySet())).willReturn(List.of(auction));

		// when
		PagedResponseDto<MyBidResponseDto> result = auctionFacade.getMyBids(memberId, null, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		MyBidResponseDto dto = result.data().get(0);
		assertThat(dto.auctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
		assertThat(dto.bidAmount()).isEqualTo(15000);
		assertThat(dto.currentPrice()).isEqualTo(15000);
	}

	/*
	@Test
	@DisplayName("나의 판매 목록 조회: 상품, 주문, 입찰수 정보를 종합하여 반환한다")
	void getMySales_Success() {
	    // given
	    Long sellerId = 1L;
	    AuctionFilterType filter = AuctionFilterType.ALL;
	    Pageable pageable = PageRequest.of(0, 10);
	
	    given(productRepository.findAllIdsBySellerId(sellerId)).willReturn(List.of(10L, 20L));
	
	    // --- Auction 1 세팅 ---
	    Auction auction1 = Auction.builder()
	        .productId(10L)
	        .sellerId(sellerId)
	        .startPrice(1000)
	        .tickSize(100)
	        .startTime(LocalDateTime.now().minusHours(1))
	        .endTime(LocalDateTime.now().plusDays(1))
	        .build();
	    // [중요] currentPrice 초기화 (int 변환 시 NPE 방지)
	    auction1.updateCurrentPrice(0); 
	    // [중요] ID 주입
	    ReflectionTestUtils.setField(auction1, "id", 100L);
	    auction1.startAuction();
	
	    // --- Auction 2 세팅 ---
	    Auction auction2 = Auction.builder()
	        .productId(20L)
	        .sellerId(sellerId)
	        .startPrice(2000)
	        .tickSize(100)
	        .startTime(LocalDateTime.now().plusDays(1))
	        .endTime(LocalDateTime.now().plusDays(2))
	        .build();
	    auction2.updateCurrentPrice(0);
	    ReflectionTestUtils.setField(auction2, "id", 200L);
	
	    given(auctionRepository.findAllByProductIdIn(anyList(), any(Pageable.class)))
	        .willReturn(new PageImpl<>(List.of(auction1, auction2)));
	
	    // --- Product 세팅 ---
	    Product product1 = Product.builder().name("Product 1").sellerId(sellerId).build();
	    ReflectionTestUtils.setField(product1, "id", 10L);
	    Product product2 = Product.builder().name("Product 2").sellerId(sellerId).build();
	    ReflectionTestUtils.setField(product2, "id", 20L);
	
	    given(productRepository.findAllByIdIn(anySet())).willReturn(List.of(product1, product2));
	
	    // --- AuctionOrder 세팅 (여기가 핵심!) ---
	    AuctionOrder order = AuctionOrder.builder()
	        .auctionId(100L)
	        .sellerId(sellerId)
	        .bidderId(2L)
	        .finalPrice(50000)
	        .build();
	    
	    // [★핵심 수정] Builder에 status가 없으므로 null 상태임 -> Reflection으로 값 주입 필수!
	    ReflectionTestUtils.setField(order, "status", AuctionOrderStatus.PROCESSING);
	
	    given(auctionOrderRepository.findAllByAuctionIdIn(anySet())).willReturn(List.of(order));
	
	    // --- Bid Count 세팅 ---
	    // Object[]의 타입이 (Long, Long)인지 확인
	    given(bidRepository.countByAuctionIdIn(anySet()))
	        .willReturn(List.of(new Object[]{100L, 5L}, new Object[]{200L, 0L}));
	
	    // when
	    PagedResponseDto<MySaleResponseDto> result = auctionFacade.getMySales(sellerId, filter, pageable);
	
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
	}
	*/
}
