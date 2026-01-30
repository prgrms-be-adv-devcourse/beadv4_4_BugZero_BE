package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionBookmark;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionBookmarkListResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionBookmarkRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionListResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionSearchCondition;
import com.bugzero.rarego.shared.auction.dto.MyAuctionOrderListResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;
import com.bugzero.rarego.shared.product.out.ProductApiClient;

@ExtendWith(MockitoExtension.class)
class AuctionReadUseCaseTest {

	@InjectMocks
	private AuctionReadUseCase auctionReadUseCase;

	@Mock
	private AuctionRepository auctionRepository;

	@Mock
	private AuctionOrderRepository auctionOrderRepository;

	@Mock
	private AuctionBookmarkRepository auctionBookmarkRepository;

	@Mock
	private BidRepository bidRepository;

	@Mock
	private ProductApiClient productApiClient;

	@Mock
	private AuctionSupport support;

	private Auction auction;
	private Long auctionId = 1L;
	private Long productId = 10L;
	private Long sellerId = 100L;

	@BeforeEach
	void setUp() {
		// Auction 생성 - 빌더에서 지원하는 필드만 사용
		auction = Auction.builder()
			.productId(productId)
			.sellerId(sellerId)
			.startPrice(10000)
			.durationDays(3)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusHours(1))
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);
		ReflectionTestUtils.setField(auction, "currentPrice", 15000);
	}

	@Test
	@DisplayName("상세 조회 - 로그인 회원(publicId 전달), 입찰 기록 있음")
	void getAuctionDetail_member_withBid() {
		// given
		String memberPublicId = "member_pub_id";
		Long memberId = 50L;
		AuctionMember member = AuctionMember.builder().publicId(memberPublicId).build();
		ReflectionTestUtils.setField(member, "id", memberId);

		// Product DTO Mocking
		ProductAuctionResponseDto productDto = ProductAuctionResponseDto.builder()
			.id(productId)
			.name("Lego StarWars")
			.description("테스트 상품 설명")
			.thumbnailUrl("lego.jpg")
			.imageUrls(List.of("img1.jpg", "img2.jpg"))
			.build();

		// Bid Mocking
		Bid highestBid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(99L) // 다른 사람이 최고 입찰자
			.bidAmount(15000)
			.build();

		Bid myLastBid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(memberId)
			.bidAmount(12000)
			.build();

		given(support.getPublicMember(memberPublicId)).willReturn(member);
		given(support.findAuctionById(auctionId)).willReturn(auction);
		given(productApiClient.getProduct(productId)).willReturn(Optional.of(productDto));
		given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)).willReturn(Optional.of(highestBid));
		given(bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, memberId)).willReturn(Optional.of(myLastBid));

		// when
		AuctionDetailResponseDto result = auctionReadUseCase.getAuctionDetail(auctionId, memberPublicId);

		// then
		assertThat(result.myParticipation().hasBid()).isTrue();
		assertThat(result.myParticipation().myLastBidPrice()).isEqualTo(12000);
		assertThat(result.productName()).isEqualTo("Lego StarWars");
		assertThat(result.imageUrls()).hasSize(2);
	}

	@Test
	@DisplayName("경매 목록 조회 - 검색 조건(키워드+상태)이 있을 때 상품 검색 후 경매 조회 수행")
	void getAuctions_with_search_condition() {
		// given
		AuctionSearchCondition condition = new AuctionSearchCondition();
		ReflectionTestUtils.setField(condition, "keyword", "키워드");
		ReflectionTestUtils.setField(condition, "category", "카테고리");
		Pageable pageable = PageRequest.of(0, 10);

		// 1. 키워드로 상품 ID 검색
		given(productApiClient.searchProductIds("키워드", "카테고리")).willReturn(List.of(productId));

		// 2. 검수 승인된 상품 ID 목록 조회
		given(productApiClient.getApprovedProductIds()).willReturn(List.of(productId));

		Page<Auction> auctionPage = new PageImpl<>(List.of(auction), pageable, 1);
		given(auctionRepository.findAllBySearchConditions(any(), any(), eq(List.of(productId)), eq(List.of(productId)), any())).willReturn(auctionPage);

		// 3. 목록 조립을 위한 상품 정보 일괄 조회 Mocking
		ProductAuctionResponseDto pDto = ProductAuctionResponseDto.builder()
			.id(productId).name("검색 상품").thumbnailUrl("img.png").build();
		given(productApiClient.getProducts(anySet())).willReturn(List.of(pDto));

		// when
		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).productName()).isEqualTo("검색 상품");
		verify(productApiClient).searchProductIds("키워드", "카테고리");
		verify(productApiClient).getApprovedProductIds();
	}

	@Test
	@DisplayName("관심 경매 조회 - 북마크된 경매 정보와 상품 정보를 조립하여 반환")
	void getMyBookmarks_success() {
		// given
		String publicId = "user_1";
		Pageable pageable = PageRequest.of(0, 10);
		AuctionMember member = AuctionMember.builder().publicId(publicId).build();
		ReflectionTestUtils.setField(member, "id", 1L);

		AuctionBookmark bookmark = AuctionBookmark.builder().memberId(1L).auctionId(auctionId).build();
		ReflectionTestUtils.setField(bookmark, "id", 1L);
		Page<AuctionBookmark> bookmarkPage = new PageImpl<>(List.of(bookmark), pageable, 1);

		// 상품 DTO 준비
		ProductAuctionResponseDto productDto = ProductAuctionResponseDto.builder()
			.id(productId)
			.name("관심 상품")
			.thumbnailUrl("fav.jpg")
			.build();

		given(support.getPublicMember(publicId)).willReturn(member);
		given(auctionBookmarkRepository.findAllByMemberId(eq(1L), any())).willReturn(bookmarkPage);
		given(auctionRepository.findAllById(any())).willReturn(List.of(auction));

		// Bulk 조회 Mocking
		given(productApiClient.getProducts(anySet())).willReturn(List.of(productDto));

		// when
		PagedResponseDto<AuctionBookmarkListResponseDto> result = auctionReadUseCase.getMyBookmarks(publicId, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		assertThat(result.data().get(0).auctionInfo().productName()).isEqualTo("관심 상품");
	}

	@Test
	@DisplayName("나의 낙찰 목록 조회 - 성공 (데이터 조립 및 정렬 확인)")
	void getMyAuctionOrders_success() {
		// given
		String memberPublicId = "bidder_id";
		AuctionMember member = AuctionMember.builder().publicId(memberPublicId).build();
		ReflectionTestUtils.setField(member, "id", 20L);

		// AuctionOrder 생성 - status는 빌더에서 지원하지 않음 (생성자에서 PROCESSING으로 설정됨)
		AuctionOrder order = AuctionOrder.builder()
			.auctionId(auctionId)
			.sellerId(sellerId)
			.bidderId(20L)
			.finalPrice(50000)
			.build();
		ReflectionTestUtils.setField(order, "id", 100L);
		ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());

		// 상품 DTO
		ProductAuctionResponseDto productDto = ProductAuctionResponseDto.builder()
			.id(productId)
			.name("Lego Titanic")
			.thumbnailUrl("thumb.jpg")
			.build();

		Page<AuctionOrder> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);

		given(support.getPublicMember(memberPublicId)).willReturn(member);
		given(auctionOrderRepository.findAllByBidderIdAndStatus(eq(20L), any(), any())).willReturn(orderPage);
		given(auctionRepository.findAllById(any())).willReturn(List.of(auction));
		given(productApiClient.getProducts(anySet())).willReturn(List.of(productDto));

		// when
		PagedResponseDto<MyAuctionOrderListResponseDto> result = auctionReadUseCase.getMyAuctionOrders(memberPublicId, null, PageRequest.of(0, 10));

		// then
		assertThat(result.data().get(0).productName()).isEqualTo("Lego Titanic");
		assertThat(result.data().get(0).finalPrice()).isEqualTo(50000);
	}

	@Test
	@DisplayName("나의 낙찰 목록 조회 - 내역이 없을 때 빈 리스트 반환 (Early Return)")
	void getMyAuctionOrders_empty() {
		// given
		String memberPublicId = "user_pub_id";
		Pageable pageable = PageRequest.of(0, 10);

		AuctionMember member = AuctionMember.builder().publicId(memberPublicId).build();
		ReflectionTestUtils.setField(member, "id", 10L);

		Page<AuctionOrder> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

		given(support.getPublicMember(memberPublicId)).willReturn(member);
		given(auctionOrderRepository.findAllByBidderIdAndStatus(eq(10L), isNull(), any(Pageable.class)))
			.willReturn(emptyPage);

		// when
		PagedResponseDto<MyAuctionOrderListResponseDto> result =
			auctionReadUseCase.getMyAuctionOrders(memberPublicId, null, pageable);

		// then
		assertThat(result.data()).isEmpty();
		// 빈 리스트이므로 productApiClient 호출되지 않음
		verify(productApiClient, never()).getProducts(anySet());
	}

	@Test
	@DisplayName("경매 목록 조회 - 성공 (조건 없음)")
	void getAuctions_success() {
		// given
		AuctionSearchCondition condition = new AuctionSearchCondition();
		Pageable pageable = PageRequest.of(0, 10);
		Page<Auction> auctionPage = new PageImpl<>(List.of(auction), pageable, 1);

		// 검수 승인된 상품 ID 목록 조회
		given(productApiClient.getApprovedProductIds()).willReturn(List.of(productId));

		given(auctionRepository.findAllBySearchConditions(any(), any(), any(), any(), any())).willReturn(auctionPage);

		ProductAuctionResponseDto pDto = ProductAuctionResponseDto.builder()
			.id(productId).name("Test Product").build();
		given(productApiClient.getProducts(anySet())).willReturn(List.of(pDto));

		// when
		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		verify(auctionRepository).findAllBySearchConditions(any(), any(), any(), any(), any());
		verify(productApiClient).getApprovedProductIds();
	}
}