package com.bugzero.rarego.boundedContext.auction.app;

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
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.out.ProductImageRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionListResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionSearchCondition;
import com.bugzero.rarego.shared.auction.dto.MyAuctionOrderListResponseDto;

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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuctionReadUseCaseTest {

	@InjectMocks
	private AuctionReadUseCase auctionReadUseCase;

	@Mock
	private AuctionRepository auctionRepository;
	@Mock
	private BidRepository bidRepository;
	@Mock
	private AuctionOrderRepository auctionOrderRepository;
	@Mock
	private ProductRepository productRepository;
	@Mock
	private AuctionMemberRepository auctionMemberRepository;
	@Mock
	private ProductImageRepository productImageRepository;

	// --- 1. 경매 상세 조회 (getAuctionDetail) 테스트 ---

	@Test
	@DisplayName("상세 조회 - 로그인 회원(publicId 전달), 입찰 기록 있음")
	void getAuctionDetail_member_withBid() {
		// given
		Long auctionId = 1L;
		String memberPublicId = "user_pub_id_1";
		Long memberId = 10L;

		// 1. 회원 조회 Mocking
		AuctionMember member = AuctionMember.builder()
			.publicId(memberPublicId)
			.build();
		ReflectionTestUtils.setField(member, "id", memberId);

		// 2. 경매 정보 (NPE 방지를 위해 int 필드 필수 설정)
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000)   // 필수
			.tickSize(100)      // 필수
			.durationDays(3)    // [수정] NPE 원인 해결
			.endTime(LocalDateTime.now().plusDays(1))
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);
		ReflectionTestUtils.setField(auction, "currentPrice", 6000);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.IN_PROGRESS);

		// 3. 입찰 정보
		Bid highestBid = Bid.builder().bidAmount(6000).bidderId(99L).build();
		Bid myLastBid = Bid.builder().bidAmount(5000).bidderId(memberId).build();

		// Mock Behavior
		given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
		given(auctionMemberRepository.findByPublicId(memberPublicId)).willReturn(Optional.of(member));
		given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)).willReturn(Optional.of(highestBid));
		given(bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, memberId))
			.willReturn(Optional.of(myLastBid));

		// when
		AuctionDetailResponseDto result = auctionReadUseCase.getAuctionDetail(auctionId, memberPublicId);

		// then
		assertThat(result.myParticipation().hasBid()).isTrue();
		assertThat(result.myParticipation().myLastBidPrice()).isEqualTo(5000);
		assertThat(result.bid().isMyHighestBid()).isFalse();
	}

	// --- 2. 낙찰 기록 상세 조회 (getAuctionOrder) 테스트 ---

	@Test
	@DisplayName("낙찰 상세 조회 - 낙찰자(BUYER)가 조회 시 정상 반환")
	void getAuctionOrder_asBuyer() {
		// given
		Long auctionId = 1L;
		String buyerPublicId = "buyer_pub_id";
		Long buyerId = 10L;
		Long sellerId = 20L;

		// 1. 회원(구매자) 조회 Mocking
		AuctionMember buyer = AuctionMember.builder()
			.publicId(buyerPublicId)
			.nickname("BuyerNick")
			.build();
		ReflectionTestUtils.setField(buyer, "id", buyerId);

		// 2. 주문 정보 (낙찰자는 buyerId)
		AuctionOrder order = AuctionOrder.builder()
			.auctionId(auctionId)
			.bidderId(buyerId)
			.sellerId(sellerId)
			.finalPrice(10000) // [수정] NPE 방지 (int 필드)
			.build();
		ReflectionTestUtils.setField(order, "id", 777L);
		ReflectionTestUtils.setField(order, "status", AuctionOrderStatus.PROCESSING);

		// 3. 관련 데이터 (경매) - NPE 방지
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000)  // [수정] 필수
			.tickSize(100)     // [수정] 필수
			.durationDays(3)   // [수정] NPE 원인 해결
			.build();
		ReflectionTestUtils.setField(auction, "id", auctionId);

		Product product = Product.builder().sellerId(sellerId).name("Test Item").build();
		ReflectionTestUtils.setField(product, "id", 50L);

		ProductImage image = ProductImage.builder().product(product).imageUrl("thumb.jpg").build();

		AuctionMember sellerMember = AuctionMember.builder()
			.id(sellerId)
			.publicId("seller_pub_id")
			.nickname("sellerNick")
			.build();

		// Mock Behavior
		given(auctionMemberRepository.findByPublicId(buyerPublicId)).willReturn(Optional.of(buyer));
		given(auctionOrderRepository.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
		given(productRepository.findById(50L)).willReturn(Optional.of(product));
		given(productImageRepository.findAllByProductId(50L)).willReturn(List.of(image));
		given(auctionMemberRepository.findById(sellerId)).willReturn(Optional.of(sellerMember));

		// when
		AuctionOrderResponseDto result = auctionReadUseCase.getAuctionOrder(auctionId, buyerPublicId);

		// then
		assertThat(result.viewerRole()).isEqualTo("BUYER");
		assertThat(result.orderId()).isEqualTo(777L);
		assertThat(result.trader().nickname()).isEqualTo("seller_pub_id");
	}

	@Test
	@DisplayName("낙찰 상세 조회 - 권한 없는 제3자가 조회 시 예외 발생")
	void getAuctionOrder_forbidden() {
		// given
		Long auctionId = 1L;
		String strangerPublicId = "stranger_pub_id";
		Long strangerId = 99L;
		Long buyerId = 10L;
		Long sellerId = 20L;

		// 1. 제3자 회원 조회 Mocking
		AuctionMember stranger = AuctionMember.builder().publicId(strangerPublicId).build();
		ReflectionTestUtils.setField(stranger, "id", strangerId);

		// 2. 주문 정보 (NPE 방지)
		AuctionOrder order = AuctionOrder.builder()
			.auctionId(auctionId)
			.bidderId(buyerId)
			.sellerId(sellerId)
			.finalPrice(10000) // [수정] NPE 원인 해결
			.build();
		ReflectionTestUtils.setField(order, "id", 777L);

		// 3. 경매 정보 (NPE 방지)
		Auction auction = Auction.builder()
			.productId(50L)
			.startPrice(1000) // [수정]
			.tickSize(100)    // [수정]
			.durationDays(3)  // [수정] NPE 원인 해결
			.build();

		// Mock Behavior
		given(auctionMemberRepository.findByPublicId(strangerPublicId)).willReturn(Optional.of(stranger));
		given(auctionOrderRepository.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

		// when & then
		assertThatThrownBy(() -> auctionReadUseCase.getAuctionOrder(auctionId, strangerPublicId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_ORDER_ACCESS_DENIED);
	}

	@Test
	@DisplayName("경매 목록 조회 - 썸네일 정렬(sortOrder) 및 데이터 조립 확인")
	void getAuctions_success() {
		// given
		AuctionSearchCondition condition = new AuctionSearchCondition(); // 조건 없음
		Pageable pageable = PageRequest.of(0, 10);

		// 1. Mock Data: 경매 (Auction)
		Long auctionId1 = 1L;
		Long productId1 = 100L;
		Auction auction1 = Auction.builder()
			.productId(productId1)
			.startPrice(1000)
			.tickSize(100)
			.durationDays(3)
			.build();
		ReflectionTestUtils.setField(auction1, "id", auctionId1);
		ReflectionTestUtils.setField(auction1, "currentPrice", 2000);
		ReflectionTestUtils.setField(auction1, "status", AuctionStatus.IN_PROGRESS);

		Page<Auction> auctionPage = new PageImpl<>(List.of(auction1), pageable, 1);

		// 2. Mock Data: 상품 (Product)
		Product product1 = Product.builder().name("Test Product").build();
		ReflectionTestUtils.setField(product1, "id", productId1);

		// 3. Mock Data: 입찰 수 (Bid Count)
		// [auctionId, count] 형태의 Object 배열 리스트
		List<Object[]> bidCounts = List.<Object[]>of(new Object[]{auctionId1, 5L});

		// 4. Mock Data: 상품 이미지 (ProductImage) - 썸네일 정렬 테스트용
		// sortOrder가 뒤죽박죽일 때, 가장 낮은 것(0)이 선택되어야 함
		ProductImage img1 = ProductImage.builder().product(product1).imageUrl("url_2.jpg").sortOrder(2).build();
		ProductImage img2 = ProductImage.builder().product(product1).imageUrl("url_0.jpg").sortOrder(0).build(); // 이게 썸네일이어야 함
		ProductImage img3 = ProductImage.builder().product(product1).imageUrl("url_1.jpg").sortOrder(1).build();

		// 5. Mocking Behavior
		given(auctionRepository.findAll(any(Specification.class), any(Pageable.class)))
			.willReturn(auctionPage);

		given(productRepository.findAllById(Set.of(productId1)))
			.willReturn(List.of(product1));

		given(bidRepository.countByAuctionIdIn(Set.of(auctionId1)))
			.willReturn(bidCounts);

		given(productImageRepository.findAllByProductIdIn(Set.of(productId1)))
			.willReturn(List.of(img1, img2, img3)); // 순서 섞어서 반환

		// when
		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		// then
		assertThat(result.data()).hasSize(1);
		AuctionListResponseDto dto = result.data().get(0);

		// 데이터 매핑 확인
		assertThat(dto.auctionId()).isEqualTo(auctionId1);
		assertThat(dto.productName()).isEqualTo("Test Product");
		assertThat(dto.currentPrice()).isEqualTo(2000);
		assertThat(dto.bidsCount()).isEqualTo(5); // 입찰 수 매핑 확인

		// [중요] 썸네일 확인: sortOrder가 0인 "url_0.jpg"가 선택되었는지
		assertThat(dto.thumbnailUrl()).isEqualTo("url_0.jpg");
	}

	@Test
	@DisplayName("경매 목록 조회 - 결과가 없을 때 빈 페이지 반환")
	void getAuctions_empty() {
		// given
		AuctionSearchCondition condition = new AuctionSearchCondition();
		Pageable pageable = PageRequest.of(0, 10);
		Page<Auction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

		given(auctionRepository.findAll(any(Specification.class), any(Pageable.class)))
			.willReturn(emptyPage);

		// when
		PagedResponseDto<AuctionListResponseDto> result = auctionReadUseCase.getAuctions(condition, pageable);

		// then
		assertThat(result.data()).isEmpty();
		assertThat(result.pageDto().totalItems()).isEqualTo(0);
	}

}